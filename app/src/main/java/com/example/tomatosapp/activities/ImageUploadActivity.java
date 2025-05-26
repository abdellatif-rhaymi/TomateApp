package com.example.tomatosapp.activities;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.tomatosapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ImageUploadActivity extends AppCompatActivity {
    private static final String TAG = "ImageUploadActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final int REQUEST_CODE_GALLERY = 100;
    private static final int REQUEST_CODE_CAMERA = 101;

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private ImageView previewImageView;
    private Button galleryButton;
    private Button cameraButton;
    private Button analyzeButton;

    private Uri selectedImageUri;

    private Button feedbackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_upload);

        previewImageView = findViewById(R.id.preview_image_view);
        galleryButton = findViewById(R.id.gallery_button);
        cameraButton = findViewById(R.id.camera_button);
        analyzeButton = findViewById(R.id.analyze_button);
        feedbackButton = findViewById(R.id.feedback_button);

        if (allPermissionsGranted()) {
            setupButtons();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        feedbackButton.setOnClickListener(v -> showFeedbackDialog());
    }

    private void setupButtons() {
        galleryButton.setOnClickListener(v -> openGallery());

        cameraButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        });

        analyzeButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                Intent intent = new Intent(this, PredictionActivity.class);
                intent.putExtra("photo_uri", selectedImageUri.toString());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Veuillez d'abord sélectionner une image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_GALLERY && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            displayImage();
        }
    }

    private void displayImage() {
        if (selectedImageUri != null) {
            Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .into(previewImageView);

            analyzeButton.setEnabled(true);
        }
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
                setupButtons();
            } else {
                Toast.makeText(this, "Permissions non accordées.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    // Dans ImageUploadActivity
    private void showFeedbackDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_feedback, null);

        RatingBar ratingBar = dialogView.findViewById(R.id.rating_bar);
        EditText feedbackEditText = dialogView.findViewById(R.id.feedback_input);

        builder.setView(dialogView)
                .setTitle("Votre feedback")
                .setPositiveButton("Envoyer", (dialog, which) -> {
                    int rating = (int) ratingBar.getRating();
                    String feedback = feedbackEditText.getText().toString();
                    saveFeedbackToFirestore(rating, feedback);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void saveFeedbackToFirestore(int rating, String feedback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("userId", user.getUid());
        feedbackData.put("userEmail", user.getEmail());
        feedbackData.put("rating", rating);
        feedbackData.put("message", feedback);
        feedbackData.put("timestamp", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("feedbacks")
                .add(feedbackData)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(this, "Merci pour votre feedback!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    }