package com.example.tomatosapp.activities;

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

import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.firestore.QueryDocumentSnapshot;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tomatosapp.R;
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
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PredictionActivity extends AppCompatActivity {
    private static final String TAG = "PredictionActivity";

    // Views
    private ImageView imageView;
    private TextView resultTextView;
    private TextView solutionsTextView;

    // Network
    private PredictionApiService apiService;
    private static final String BASE_URL = "https://tomato-disease-service-299287005031.europe-west1.run.app/";

    // Threading
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    // Data
    private Uri photoUri;

    private FirebaseFirestore db;

    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prediction);
        Log.d(TAG, "onCreate: Activity created");

        // Initialize views
        imageView = findViewById(R.id.prediction_image_view);
        resultTextView = findViewById(R.id.result_text_view);
        solutionsTextView = findViewById(R.id.solutions_text_view);
        backButton  = findViewById(R.id.back_button);
        Log.d(TAG, "Views initialized");

        // Initialize Firebase
        try {
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firestore initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Firestore initialization failed", e);
            showError("Erreur d'initialisation de la base de données");
            return;
        }

        // Configure Retrofit
        configureRetrofit();

        // Handle incoming image
        handleIncomingImage();

        // back to another analyse

        backButton.setOnClickListener(v->{finish();});
    }

    private void configureRetrofit() {
        Log.d(TAG, "Configuring Retrofit...");
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> Log.d(TAG, "HTTP: " + message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(PredictionApiService.class);
        Log.d(TAG, "Retrofit configured successfully");
    }

    private void handleIncomingImage() {
        Log.d(TAG, "Handling incoming image...");
        String uriString = getIntent().getStringExtra("photo_uri");

        if (uriString == null) {
            Log.e(TAG, "No photo URI found in intent extras");
            showError("Aucune image trouvée");
            finish();
            return;
        }

        try {
            photoUri = Uri.parse(uriString);
            Log.d(TAG, "Photo URI parsed: " + photoUri.toString());
            displayImage();
            uploadAndAnalyzeImage(photoUri);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing image URI", e);
            showError("Format d'image invalide");
            finish();
        }
    }

    private void displayImage() {
        Log.d(TAG, "Displaying image...");
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
            imageView.setImageBitmap(bitmap);
            Log.d(TAG, "Image displayed successfully");
        } catch (IOException e) {
            Log.e(TAG, "Error loading image", e);
            showError("Erreur de chargement de l'image");
        }
    }

    private void uploadAndAnalyzeImage(Uri imageUri) {
        Log.d(TAG, "Starting image upload and analysis...");
        updateStatus("Analyse en cours...", "Recherche de solutions...");

        executorService.execute(() -> {
            Log.d(TAG, "Executor service started for image processing");
            try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                if (inputStream == null) {
                    Log.e(TAG, "Input stream is null");
                    showError("Impossible d'ouvrir l'image");
                    return;
                }

                Log.d(TAG, "Converting image to byte array...");
                byte[] imageBytes = convertInputStreamToByteArray(inputStream);
                Log.d(TAG, "Image size: " + imageBytes.length + " bytes");

                RequestBody requestBody = createRequestBody(imageUri, imageBytes);
                MultipartBody.Part imagePart = createMultipartBody(requestBody);

                Log.d(TAG, "Making API call...");
                apiService.uploadImage(imagePart).enqueue(new Callback<PredictionResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<PredictionResponse> call, @NonNull Response<PredictionResponse> response) {
                        Log.d(TAG, "API response received. Code: " + response.code());
                        handleApiResponse(response);
                    }

                    @Override
                    public void onFailure(@NonNull Call<PredictionResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "API call failed", t);
                        showError("Échec de connexion: " + t.getMessage());
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error during image processing", e);
                showError("Erreur de traitement: " + e.getMessage());
            }
        });
    }

    private byte[] convertInputStreamToByteArray(InputStream inputStream) throws IOException {
        Log.d(TAG, "Converting input stream to byte array...");
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 4]; // 4KB buffer
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        Log.d(TAG, "Byte array conversion complete");
        return byteBuffer.toByteArray();
    }

    private RequestBody createRequestBody(Uri imageUri, byte[] imageBytes) {
        String mimeType = getContentResolver().getType(imageUri);
        if (mimeType == null) {
            mimeType = "image/jpeg";
            Log.w(TAG, "MIME type not found, defaulting to JPEG");
        }
        Log.d(TAG, "Creating request body with MIME type: " + mimeType);
        return RequestBody.create(MediaType.parse(mimeType), imageBytes);
    }

    private MultipartBody.Part createMultipartBody(RequestBody requestFile) {
        Log.d(TAG, "Creating multipart body");
        return MultipartBody.Part.createFormData("file", "image.jpg", requestFile);
    }

    private void handleApiResponse(Response<PredictionResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
            PredictionResponse prediction = response.body();
            if (prediction.getError() != null) {
                Log.e(TAG, "API returned error: " + prediction.getError());
                showError(prediction.getError());
            } else {
                Log.d(TAG, "Prediction successful. Label: " + prediction.getPredictedLabel() +
                        ", Confidence: " + prediction.getConfidence());
                displayPredictionResults(prediction);
            }
        } else {
            String errorBody = "Empty error body";
            try {
                if (response.errorBody() != null) {
                    errorBody = response.errorBody().string();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading error body", e);
            }
            Log.e(TAG, "API request failed. Code: " + response.code() + ", Error: " + errorBody);
            showError("Erreur du serveur: " + response.code());
        }
    }

    private void displayPredictionResults(PredictionResponse prediction) {
        String diseaseName = prediction.getPredictedLabel().trim();
        float confidence = prediction.getConfidence();

        Log.d(TAG, "Displaying results for disease: '" + diseaseName + "'");
        Log.d(TAG, "Confidence: " + confidence);

        // Format the result text
        String resultText = String.format(Locale.getDefault(),
                "Maladie détectée: %s\nConfiance: %.1f%%",
                diseaseName,
                confidence * 100);

        // Update UI on main thread
        mainThreadHandler.post(() -> {
            resultTextView.setText(resultText);
            solutionsTextView.setText("Recherche de solutions en cours...");
        });

        // Fetch solution for the detected disease
        fetchSolutionForDisease(diseaseName);
    }
    private void fetchSolutionForDisease(String diseaseName) {
        Log.d(TAG, "=== STARTING FIRESTORE SOLUTION FETCH ===");
        Log.d(TAG, "Original disease name: '" + diseaseName + "'");
        Log.d(TAG, "Disease name length: " + diseaseName.length());

        // Clean the disease name - remove extra spaces and normalize
        String cleanDiseaseName = diseaseName.trim();
        Log.d(TAG, "Cleaned disease name: '" + cleanDiseaseName + "'");

        // Update UI to show we're searching
        mainThreadHandler.post(() -> {
            solutionsTextView.setText("🔍 Recherche en cours pour: " + cleanDiseaseName);
        });

        // First, try to get the document with exact name match
        db.collection("maladies")
                .document(cleanDiseaseName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "=== FIRESTORE EXACT MATCH QUERY RESULT ===");
                    Log.d(TAG, "Document exists: " + documentSnapshot.exists());
                    Log.d(TAG, "Document ID: " + documentSnapshot.getId());

                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "✅ EXACT MATCH FOUND!");

                        String solution = documentSnapshot.getString("solution");
                        Log.d(TAG, "Solution value: " + (solution != null ? "Found (" + solution.length() + " chars)" : "null"));

                        if (solution != null && !solution.trim().isEmpty()) {
                            Log.d(TAG, "✅ SOLUTION RETRIEVED SUCCESSFULLY");
                            Log.d(TAG, "Solution preview: " + solution.substring(0, Math.min(100, solution.length())));

                            mainThreadHandler.post(() -> {
                                solutionsTextView.setText("✅ Solution trouvée:\n\n" + solution);
                            });
                            return;
                        } else {
                            Log.w(TAG, "⚠️ Solution field is null or empty");
                        }
                    }

                    // If exact match failed, try searching all documents
                    Log.d(TAG, "❌ Exact match failed, searching all documents...");
                    searchAllMaladiesDocuments(cleanDiseaseName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Exact match query failed", e);
                    // Try searching all documents as fallback
                    searchAllMaladiesDocuments(cleanDiseaseName);
                });
    }

    private void searchAllMaladiesDocuments(String originalDiseaseName) {
        Log.d(TAG, "=== SEARCHING ALL MALADIES DOCUMENTS ===");
        Log.d(TAG, "Search term: '" + originalDiseaseName + "'");

        db.collection("maladies")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "=== FIRESTORE COLLECTION QUERY SUCCESSFUL ===");
                    Log.d(TAG, "Total documents found: " + queryDocumentSnapshots.size());

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.e(TAG, "❌ NO DOCUMENTS FOUND in maladies collection");
                        mainThreadHandler.post(() -> {
                            solutionsTextView.setText("❌ Aucune maladie trouvée dans la base de données");
                        });
                        return;
                    }

                    // Log all available diseases and try to find matches
                    boolean foundMatch = false;
                    int documentCount = 0;

                    Log.d(TAG, "=== ALL AVAILABLE DISEASES IN FIRESTORE ===");
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        documentCount++;
                        String documentId = document.getId();
                        String solution = document.getString("solution");

                        Log.d(TAG, documentCount + ". Disease ID: '" + documentId + "'");
                        Log.d(TAG, "   - Has solution: " + (solution != null && !solution.trim().isEmpty()));

                        if (solution != null && !solution.trim().isEmpty()) {
                            Log.d(TAG, "   - Solution preview: " + solution.substring(0, Math.min(50, solution.length())) + "...");
                        }

                        // Try different matching strategies
                        boolean exactMatch = documentId.equals(originalDiseaseName);
                        boolean caseInsensitiveMatch = documentId.toLowerCase().equals(originalDiseaseName.toLowerCase());
                        boolean containsMatch = documentId.toLowerCase().contains(originalDiseaseName.toLowerCase());
                        boolean reverseContainsMatch = originalDiseaseName.toLowerCase().contains(documentId.toLowerCase());
                        boolean normalizedMatch = normalizeDiseaseName(documentId).equals(normalizeDiseaseName(originalDiseaseName));

                        Log.d(TAG, "   - Exact match: " + exactMatch);
                        Log.d(TAG, "   - Case insensitive: " + caseInsensitiveMatch);
                        Log.d(TAG, "   - Contains match: " + containsMatch);
                        Log.d(TAG, "   - Reverse contains: " + reverseContainsMatch);
                        Log.d(TAG, "   - Normalized match: " + normalizedMatch);

                        if (exactMatch || caseInsensitiveMatch || containsMatch || reverseContainsMatch || normalizedMatch) {
                            Log.d(TAG, "✅ MATCH FOUND: " + documentId);

                            if (solution != null && !solution.trim().isEmpty()) {
                                Log.d(TAG, "✅ SOLUTION FOUND FOR MATCH");

                                final String matchType = exactMatch ? "exact" :
                                        caseInsensitiveMatch ? "case-insensitive" :
                                                containsMatch ? "contains" :
                                                        reverseContainsMatch ? "reverse-contains" : "normalized";

                                mainThreadHandler.post(() -> {
                                    solutionsTextView.setText("✅ Solution trouvée (" + matchType + " match):\n\n" + solution);
                                });
                                foundMatch = true;
                                break;
                            } else {
                                Log.w(TAG, "⚠️ Match found but solution is null/empty");
                            }
                        }
                    }

                    if (!foundMatch) {
                        Log.e(TAG, "❌ NO MATCHES FOUND in " + documentCount + " documents");

                        // Show available diseases for debugging
                        StringBuilder availableDiseases = new StringBuilder("Maladies disponibles:\n");
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            availableDiseases.append("• ").append(document.getId()).append("\n");
                        }

                        mainThreadHandler.post(() -> {
                            solutionsTextView.setText("❌ Aucune solution trouvée pour: '" + originalDiseaseName + "'\n\n" + availableDiseases.toString());
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ FIRESTORE COLLECTION QUERY FAILED", e);
                    mainThreadHandler.post(() -> {
                        solutionsTextView.setText("❌ Erreur Firestore: " + e.getMessage());
                    });
                });
    }

    // Alternative method: Search by field value instead of document ID
    private void searchBySolutionField(String diseaseName) {
        Log.d(TAG, "=== SEARCHING BY FIELD VALUE ===");

        // If your documents have a "name" field, use this approach
        db.collection("maladies")
                .whereEqualTo("name", diseaseName) // Adjust field name as needed
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        String solution = document.getString("solution");

                        if (solution != null && !solution.trim().isEmpty()) {
                            Log.d(TAG, "✅ Solution found by field search");
                            mainThreadHandler.post(() -> {
                                solutionsTextView.setText("✅ Solution trouvée:\n\n" + solution);
                            });
                        }
                    } else {
                        Log.d(TAG, "No documents found with name field matching: " + diseaseName);
                        // Fallback to document ID search
                        searchAllMaladiesDocuments(diseaseName);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Field search failed", e);
                    // Fallback to document ID search
                    searchAllMaladiesDocuments(diseaseName);
                });
    }

    private String normalizeDiseaseName(String diseaseName) {
        if (diseaseName == null) return "";

        String normalized = diseaseName.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ")  // Replace multiple spaces with single space
                .replaceAll("[^a-z0-9\\s]", "");  // Remove special characters except spaces

        Log.d(TAG, "Normalized '" + diseaseName + "' -> '" + normalized + "'");
        return normalized;
    }

    private void updateStatus(String result, String solution) {
        Log.d(TAG, "Updating status. Result: " + result + ", Solution: " + solution);
        mainThreadHandler.post(() -> {
            resultTextView.setText(result);
            solutionsTextView.setText(solution);
        });
    }

    private void showError(String message) {
        Log.e(TAG, "Showing error: " + message);
        mainThreadHandler.post(() -> {
            resultTextView.setText("Erreur: " + message);
            solutionsTextView.setText("Impossible d'afficher les solutions");
            Toast.makeText(PredictionActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed");
        executorService.shutdown();
    }
}