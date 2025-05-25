package com.example.tomatosapp.activities; // Adapte ton package

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button; // Importe Button si tu as analyzeAgainButton
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.tomatosapp.BuildConfig; // Adapte si ton package est différent

import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.TimeUnit; // Ajoute cet import

// ... dans onCreate, avant de créer Retrofit ...


import com.example.tomatosapp.R; // Adapte ton package R
// Importe les nouvelles classes réseau
import com.example.tomatosapp.network.PredictionApiService;
import com.example.tomatosapp.network.PredictionResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor; // Pour le logging des requêtes
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit; // Ajoute cet import

// ... dans onCreate, avant de créer Retrofit ...

public class PredictionActivity extends AppCompatActivity {
    private static final String TAG = "PredictionActivity";

    private ImageView imageView;
    private TextView resultTextView;
    // private Button analyzeAgainButton; // Décommente si tu l'utilises

    private Uri photoUri; // URI passée par l'intent

    // --- NOUVEAU : Pour l'appel API ---
    private PredictionApiService apiService;
    // !!! REMPLACE PAR TON URL CLOUD RUN EXACTE !!!
    //private static final String BASE_URL = "https://tomato-disease-service-299287005031.europe-west1.run.app/";

    // Executor pour les tâches d'arrière-plan (comme la préparation de l'image)
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    // Handler pour poster les résultats sur le thread UI
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private static final String BASE_URL = BuildConfig.API_BASE_URL;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prediction);
        imageView = findViewById(R.id.prediction_image_view);
        resultTextView = findViewById(R.id.result_text_view);
        // analyzeAgainButton = findViewById(R.id.analyze_again_button); // Décommente si besoin

        // Initialisation de Retrofit
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // Loggue les détails des requêtes/réponses

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(60, TimeUnit.SECONDS) // Timeout de connexion: 60 secondes
                .readTimeout(60, TimeUnit.SECONDS)    // Timeout de lecture: 60 secondes
                .writeTimeout(60, TimeUnit.SECONDS)   // Timeout d'écriture: 60 secondes
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL) // <- Utilise la variable ici
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(PredictionApiService.class);

        // --- FIN Initialisation Retrofit ---

        String uriString = getIntent().getStringExtra("photo_uri");
        if (uriString != null) {
            photoUri = Uri.parse(uriString);
            Log.d(TAG, "URI Reçue: " + photoUri.toString());
            displayImage();
            uploadAndAnalyzeImage(photoUri); // Lance l'appel API
        } else {
            Log.e(TAG, "Erreur: Aucun URI d'image trouvé dans l'intent.");
            Toast.makeText(this, "Erreur: Aucune image trouvée", Toast.LENGTH_SHORT).show();
            finish();
        }

        // if (analyzeAgainButton != null) {
        //     analyzeAgainButton.setOnClickListener(v -> finish());
        // }
    }

    private void displayImage() {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
            imageView.setImageBitmap(bitmap);
            Log.d(TAG, "Image affichée depuis l'URI.");
        } catch (IOException e) {
            Log.e(TAG, "Erreur de chargement de l'image depuis l'URI: " + e.getMessage());
            Toast.makeText(this, "Erreur de chargement de l'image", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadAndAnalyzeImage(Uri imageUri) {
        resultTextView.setText("Envoi de l'image au serveur...");

        executorService.execute(() -> { // Exécute la préparation et l'appel en arrière-plan
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    mainThreadHandler.post(() -> {
                        resultTextView.setText("Erreur: Impossible d'ouvrir le flux de l'image.");
                        Toast.makeText(PredictionActivity.this, "Erreur lecture image", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Convertir InputStream en byte array
                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                int bufferSize = 1024 * 4; // Augmenter la taille du buffer peut aider
                byte[] buffer = new byte[bufferSize];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    byteBuffer.write(buffer, 0, len);
                }
                byte[] imageBytes = byteBuffer.toByteArray();
                inputStream.close(); // Ferme le flux

                // Créer la partie de la requête pour le fichier
                // Essaye de récupérer le type MIME, sinon utilise "image/jpeg" ou "image/png" par défaut
                String mimeType = getContentResolver().getType(imageUri);
                if (mimeType == null) {
                    mimeType = "image/jpeg"; // Un type par défaut
                }
                RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), imageBytes);
                MultipartBody.Part body = MultipartBody.Part.createFormData("file", "image.jpg", requestFile); // "file" doit correspondre à la clé attendue par Flask

                // Faire l'appel API
                Call<PredictionResponse> call = apiService.uploadImage(body);
                call.enqueue(new Callback<PredictionResponse>() {
                    @Override
                    public void onResponse(Call<PredictionResponse> call, Response<PredictionResponse> response) {
                        mainThreadHandler.post(() -> { // Toujours mettre à jour l'UI sur le thread principal
                            if (response.isSuccessful() && response.body() != null) {
                                PredictionResponse prediction = response.body();
                                if (prediction.getError() != null) {
                                    resultTextView.setText("Erreur du serveur: " + prediction.getError());
                                    Log.e(TAG, "Erreur serveur: " + prediction.getError());
                                } else {
                                    String label = prediction.getPredictedLabel();
                                    float confidence = prediction.getConfidence(); // La réponse JSON donne déjà la confiance brute
                                    String confidencePercent = String.format(Locale.US, "%.1f%%", confidence * 100);
                                    resultTextView.setText("Résultat (Cloud):\n" + label + "\nConfiance: " + confidencePercent);
                                    Log.i(TAG, "Prédiction Cloud: " + label + " (" + confidencePercent + ")");
                                    // --- TODO: ICI, AJOUTER LA LOGIQUE POUR CHERCHER ET AFFICHER LA SOLUTION ---
                                    // String solutionInfo = findSolutionForDisease(label);
                                    // // Ajoute à resultTextView ou un autre TextView
                                }
                            } else {
                                String errorBodyStr = "Réponse d'erreur non disponible";
                                if (response.errorBody() != null) {
                                    try {
                                        errorBodyStr = response.errorBody().string();
                                    } catch (IOException e) {
                                        Log.e(TAG, "Erreur lecture errorBody", e);
                                    }
                                }
                                Log.e(TAG, "Erreur API: Code " + response.code() + " - Body: " + errorBodyStr);
                                resultTextView.setText("Erreur API: " + response.code());
                                Toast.makeText(PredictionActivity.this, "Erreur serveur: " + response.code(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<PredictionResponse> call, Throwable t) {
                        mainThreadHandler.post(() -> {
                            Log.e(TAG, "Échec de l'appel API (réseau/autre)", t);
                            resultTextView.setText("Échec réseau: " + t.getMessage());
                            Toast.makeText(PredictionActivity.this, "Problème de connexion", Toast.LENGTH_LONG).show();
                        });
                    }
                });

            } catch (IOException e) {
                mainThreadHandler.post(() -> {
                    Log.e(TAG, "Erreur IO lors de la préparation de l'image pour l'envoi: " + e.getMessage());
                    resultTextView.setText("Erreur préparation image.");
                    Toast.makeText(PredictionActivity.this, "Erreur préparation image", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // --- TODO: Fonction pour récupérer les infos sur la maladie (base de connaissances locale) ---
    // private String findSolutionForDisease(String diseaseName) {
    //     // Implémente la lecture de ton JSON ou autre structure ici
    //     // et retourne la description/solution.
    //     return "Informations sur le traitement/prévention à venir...";
    // }
    // -------------------------------------------------------------


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Arrête l'executor pour éviter les fuites de threads
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
