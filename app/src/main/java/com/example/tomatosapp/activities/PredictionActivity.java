package com.example.tomatosapp.activities; // Adapte ton package

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Pour l'executor

import com.example.tomatosapp.R; // Adapte ton package R
import com.example.tomatosapp.utils.TomatoDiseaseClassifier; // Adapte l'import

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Renomme l'activité si besoin (ici PredictionActivity)
public class PredictionActivity extends AppCompatActivity {
    private static final String TAG = "PredictionActivity";

    private ImageView imageView;
    private TextView resultTextView;
    private Button analyzeAgainButton; // Si tu as ce bouton

    private TomatoDiseaseClassifier classifier;
    private Uri photoUri; // URI passée par l'intent

    // Executor pour exécuter l'inférence en arrière-plan
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    // Handler pour poster les résultats sur le thread UI
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prediction); // Ton layout pour afficher le résultat

        imageView = findViewById(R.id.image_view); // ID dans ton layout
        resultTextView = findViewById(R.id.result_text_view); // ID dans ton layout
        // analyzeAgainButton = findViewById(R.id.analyze_again_button); // ID si tu as ce bouton

        // Récupérer l'URI de la photo depuis l'Intent
        String uriString = getIntent().getStringExtra("photo_uri"); // Assure-toi que l'activité précédente passe bien cet extra
        if (uriString != null) {
            photoUri = Uri.parse(uriString);
            Log.d(TAG, "URI Reçue: " + photoUri.toString());
            displayImage(); // Affiche l'image
            setupTFLiteClassifierAndAnalyze(); // Initialise ET analyse
        } else {
            Log.e(TAG, "Erreur: Aucun URI d'image trouvé dans l'intent.");
            Toast.makeText(this, "Erreur: Aucune image trouvée", Toast.LENGTH_SHORT).show();
            finish(); // Ferme l'activité si pas d'image
            return;
        }

        // Optionnel: Configurer un bouton pour revenir en arrière
        // analyzeAgainButton.setOnClickListener(v -> finish());
    }

    private void displayImage() {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
            imageView.setImageBitmap(bitmap);
            Log.d(TAG,"Image affichée depuis l'URI.");
        } catch (IOException e) {
            Log.e(TAG, "Erreur de chargement de l'image depuis l'URI: " + e.getMessage());
            Toast.makeText(this, "Erreur de chargement de l'image", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupTFLiteClassifierAndAnalyze() {
        resultTextView.setText("Initialisation du modèle...");
        // Utilise l'executor pour initialiser en arrière-plan au cas où ça prendrait du temps
        executorService.execute(() -> {
            try {
                classifier = new TomatoDiseaseClassifier(this); // Initialise ici
                // Si l'initialisation réussit, lance l'analyse
                if (classifier.isModelReady()) {
                    analyzeImage();
                } else {
                    // Gère le cas où isModelReady est false après l'init (ex: incohérence labels)
                    mainThreadHandler.post(() -> {
                        resultTextView.setText("Erreur: Modèle non initialisé correctement.");
                        Toast.makeText(this, "Erreur chargement modèle (vérifier logs)", Toast.LENGTH_LONG).show();
                    });
                }
            } catch (IOException e) {
                // Gère l'IOException lancée par le constructeur si échec critique
                mainThreadHandler.post(() -> {
                    Log.e(TAG, "Erreur critique lors du chargement du modèle: " + e.getMessage());
                    resultTextView.setText("Erreur chargement modèle IA.");
                    Toast.makeText(this, "Erreur critique chargement modèle IA", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void analyzeImage() {
        if (classifier == null || !classifier.isModelReady()) {
            mainThreadHandler.post(() -> resultTextView.setText("Erreur: Modèle non prêt."));
            return;
        }
        mainThreadHandler.post(() -> resultTextView.setText("Analyse de l'image..."));

        // Exécute l'analyse sur un thread séparé
        executorService.execute(() -> {
            Bitmap bitmap = null;
            try {
                // Re-charge le bitmap ici au cas où il serait gros
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                if (bitmap == null) throw new IOException("Impossible de charger le Bitmap");

                // Effectuer la prédiction
                final List<TomatoDiseaseClassifier.Recognition> results = classifier.recognizeImage(bitmap);

                // Afficher les résultats sur le thread UI
                mainThreadHandler.post(() -> displayResults(results));

            } catch (IOException e) {
                mainThreadHandler.post(() -> {
                    Log.e(TAG, "Erreur IO lors de l'analyse: " + e.getMessage());
                    resultTextView.setText("Erreur lors de l'analyse.");
                    Toast.makeText(this, "Erreur analyse image", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) { // Attrape d'autres erreurs potentielles d'inférence
                mainThreadHandler.post(() -> {
                    Log.e(TAG, "Erreur Inférence: " + e.getMessage());
                    resultTextView.setText("Erreur Inférence Modèle.");
                    Toast.makeText(this, "Erreur Inférence", Toast.LENGTH_SHORT).show();
                });
            } finally {
                // Optionnel: libérer le bitmap s'il est très gros et non nécessaire ailleurs
                // if (bitmap != null) {
                //     bitmap.recycle();
                // }
            }
        });
    }


    private void displayResults(List<TomatoDiseaseClassifier.Recognition> results) {
        if (results == null) { // Vérifie null au cas où recognizeImage retourne null (ne devrait pas avec le code actuel)
            resultTextView.setText("Erreur: Résultat d'analyse invalide.");
            return;
        }
        if (results.isEmpty()) {
            resultTextView.setText("Aucune maladie détectée avec une confiance suffisante.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        // N'affiche que le meilleur résultat basé sur le code actuel de postprocess
        TomatoDiseaseClassifier.Recognition topResult = results.get(0);
        String disease = topResult.getTitle();
        float confidence = topResult.getConfidence() * 100;

        sb.append("Résultat:\n");
        sb.append(disease).append(" (").append(String.format(Locale.US, "%.1f%%", confidence)).append(")\n\n");

        // --- TODO: ICI, AJOUTER LA LOGIQUE POUR CHERCHER ET AFFICHER LA SOLUTION ---
        // String solutionInfo = findSolutionForDisease(disease); // Fonction à créer
        // sb.append("Recommandation:\n").append(solutionInfo);
        // -------------------------------------------------------------------------

        resultTextView.setText(sb.toString());
    }

    // --- TODO: Fonction pour récupérer les infos sur la maladie ---
    // private String findSolutionForDisease(String diseaseName) {
    //     // Implémente la lecture de ton JSON ou autre structure ici
    //     // et retourne la description/solution.
    //     return "Informations sur le traitement/prévention à venir...";
    // }
    // -------------------------------------------------------------


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Arrête l'executor et ferme le classificateur
        executorService.shutdown();
        if (classifier != null) {
            classifier.close();
        }
    }
}