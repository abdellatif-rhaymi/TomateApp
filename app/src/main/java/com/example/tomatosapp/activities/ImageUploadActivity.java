package com.example.tomatosapp.activities;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.tomatosapp.R;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_upload);

        previewImageView = findViewById(R.id.preview_image_view);
        galleryButton = findViewById(R.id.gallery_button);
        cameraButton = findViewById(R.id.camera_button);
        analyzeButton = findViewById(R.id.analyze_button);

        if (allPermissionsGranted()) {
            setupButtons();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
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
    }