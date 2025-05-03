package com.example.tomatosapp.activities;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.tomatosapp.R;
import com.google.common.util.concurrent.ListenableFuture;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private Button captureButton;
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        viewFinder = findViewById(R.id.viewFinder);
        captureButton = findViewById(R.id.camera_capture_button);

        // Vérifier les permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Configuration du bouton de capture
        captureButton.setOnClickListener(v -> takePhoto());

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void takePhoto() {
        if (imageCapture == null) {
            return;
        }

        // Créer un nom de fichier pour l'image
        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        // Créer l'output pour l'image
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues).build();

        // Prendre la photo
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = outputFileResults.getSavedUri();
                        String msg = "Photo capture succeeded: " + savedUri;
                        Toast.makeText(CameraActivity.this, msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg);

                        // Passer à l'activité d'analyse
                        Intent intent = new Intent(CameraActivity.this, PredictionActivity.class);
                        intent.putExtra("photo_uri", savedUri.toString());
                        startActivity(intent);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                        Toast.makeText(CameraActivity.this, "Échec de la capture", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Lier le cycle de vie de la caméra au cycle de vie de l'activité
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Créer un aperçu
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // Configurer l'image capture
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // Sélectionner la caméra arrière par défaut
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Lier les cas d'utilisation à la caméra
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Échec de la liaison de la caméra: ", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions non accordées.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
