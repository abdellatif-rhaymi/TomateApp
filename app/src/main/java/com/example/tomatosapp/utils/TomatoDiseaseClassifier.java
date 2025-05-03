package com.example.tomatosapp.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TomatoDiseaseClassifier {
    private static final String TAG = "TomatoDiseaseClassifier";

    // --- CONFIGURATION À VÉRIFIER ---
    // Doit correspondre au fichier dans assets
    private static final String MODEL_FILE = "tomato_disease_model_tf214_quant_from_h5.tflite";

    // Doit correspondre à l'ordre de sortie du modèle
    private static final List<String> LABELS = Arrays.asList(
            "Tomato Bacterial spot",
            "Tomato Early blight",
            "Tomato Healthy",
            "Tomato Late blight",
            "Tomato Leaf Mold",
            "Tomato Mosaic virus",
            "Tomato Septoria leaf spot",
            "Tomato Spider mites",
            "Tomato Target Spot",
            "Tomato Yellow Leaf Curl Virus"
    );
    // --------------------------------

    private static final int INPUT_IMAGE_WIDTH = 224;
    private static final int INPUT_IMAGE_HEIGHT = 224;
    private static final float PROBABILITY_THRESHOLD = 0.1f;

    private Interpreter tflite;
    private boolean isModelLoaded = false;
    private TensorImage inputImageBuffer;
    private TensorBuffer outputProbabilityBuffer;

    public static class Recognition {
        private final String id; private final String title; private final Float confidence;
        public Recognition(String id, String title, Float confidence) { this.id = id; this.title = title; this.confidence = confidence; }
        public String getId() { return id; } public String getTitle() { return title; } public Float getConfidence() { return confidence; }
        @Override public String toString() { return String.format(Locale.US, "Label: %s, Confidence: %.2f%%", title, confidence * 100.0f); }
    }

    public TomatoDiseaseClassifier(Context context) throws IOException {
        // Supprimez la vérification de version qui cause des problèmes
        // et continuez directement avec le chargement du modèle
        Log.i(TAG, "Initialisation du classificateur...");

        MappedByteBuffer modelFile = null;
        try {
            modelFile = loadModelFile(context, MODEL_FILE);
        } catch (IOException e) {
            isModelLoaded = false; Log.e(TAG, "Error loading model file: " + e.getMessage()); throw e;
        }

        try {
            Interpreter.Options options = new Interpreter.Options();
            // Vous pouvez ajuster le nombre de threads si nécessaire
            options.setNumThreads(2);

            Log.d(TAG, "Tentative d'initialisation de l'Interpreter...");
            tflite = new Interpreter(modelFile, options);
            Log.d(TAG, "Interpreter initialisé.");

            int outputTensorSize = tflite.getOutputTensor(0).shape()[1];
            if (outputTensorSize != LABELS.size()) {
                Log.e(TAG,"Incohérence labels ("+LABELS.size()+") / sortie modèle ("+outputTensorSize+").");
                isModelLoaded = false; close(); throw new IOException("Incohérence labels/sortie modèle.");
            }

            int[] inputShape = tflite.getInputTensor(0).shape();
            DataType inputDataType = tflite.getInputTensor(0).dataType();
            inputImageBuffer = new TensorImage(inputDataType);
            int[] outputShape = tflite.getOutputTensor(0).shape();
            DataType outputDataType = tflite.getOutputTensor(0).dataType();
            outputProbabilityBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType);

            Log.d(TAG, "Buffers Input/Output créés.");
            isModelLoaded = true; // Succès

        } catch (Exception e) {
            isModelLoaded = false;
            Log.e(TAG, "ERREUR FINALE LORS DE L'INITIALISATION DE L'INTERPRETER: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Impossible d'initialiser l'Interpréteur TensorFlow Lite.", e);
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelFileName) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        // Ferme les ressources proprement
        try { fileChannel.close(); } catch (IOException ignored) {}
        try { inputStream.close(); } catch (IOException ignored) {}
        try { fileDescriptor.close(); } catch (IOException ignored) {}
        return buffer;
    }

    private TensorImage preprocessImage(Bitmap bitmap) {
        inputImageBuffer.load(bitmap);
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(INPUT_IMAGE_HEIGHT, INPUT_IMAGE_WIDTH, ResizeOp.ResizeMethod.BILINEAR))
                .add(getNormalizeOp()) // Normalisation [0, 1]
                .build();
        return imageProcessor.process(inputImageBuffer);
    }

    private TensorOperator getNormalizeOp() {
        return new NormalizeOp(0.0f, 255.0f);
    }

    public boolean isModelReady() { return isModelLoaded; }

    public List<Recognition> recognizeImage(Bitmap bitmap) {
        if (!isModelLoaded || tflite == null || inputImageBuffer == null || outputProbabilityBuffer == null) {
            Log.e(TAG, "Cannot recognize image: Classifier not properly initialized.");
            return new ArrayList<>();
        }
        long startTime = System.currentTimeMillis();
        preprocessImage(bitmap); // Met à jour inputImageBuffer
        Log.d(TAG, "Exécution de l'inférence...");
        try {
            // Utilise runForMultipleInputsOutputs (devrait être compatible)
            Object[] inputs = {inputImageBuffer.getBuffer()};
            Map<Integer, Object> outputs = new HashMap<>();
            outputs.put(0, outputProbabilityBuffer.getBuffer().rewind());
            tflite.runForMultipleInputsOutputs(inputs, outputs);
        } catch (Exception e) {
            Log.e(TAG, "Erreur pendant l'inférence TFLite (runForMultipleInputsOutputs)", e);
            return new ArrayList<>();
        }
        Log.d(TAG, "Inférence terminée.");
        List<Recognition> recognitions = postprocess(outputProbabilityBuffer.getFloatArray());
        long endTime = System.currentTimeMillis();
        Log.i(TAG, "Temps total de classification: " + (endTime - startTime) + " ms");
        return recognitions;
    }

    private List<Recognition> postprocess(float[] probabilities) {
        List<Recognition> recognitions = new ArrayList<>();
        for (int i = 0; i < LABELS.size(); i++) {
            float confidence = probabilities[i];
            if (confidence >= PROBABILITY_THRESHOLD) {
                recognitions.add(new Recognition("" + i, LABELS.get(i), confidence));
            }
            Log.v(TAG, String.format("Classe %d (%s): %.4f", i, LABELS.get(i), confidence));
        }
        Recognition bestRecognition = null;
        float maxConfidence = 0f;
        for (Recognition rec : recognitions) {
            if (rec.getConfidence() > maxConfidence) {
                maxConfidence = rec.getConfidence();
                bestRecognition = rec;
            }
        }
        List<Recognition> topResult = new ArrayList<>();
        if (bestRecognition != null) {
            Log.d(TAG, "Meilleure prédiction: " + bestRecognition.toString());
            topResult.add(bestRecognition);
        } else {
            Log.d(TAG, "Aucune prédiction au dessus du seuil " + PROBABILITY_THRESHOLD);
        }
        return topResult;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            isModelLoaded = false;
            Log.d(TAG, "Classificateur TFLite fermé.");
        }
    }
}