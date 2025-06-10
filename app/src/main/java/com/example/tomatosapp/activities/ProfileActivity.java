package com.example.tomatosapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.tomatosapp.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private DocumentReference userRef;
    private FirebaseUser currentUser;

    private static final int REQUEST_CODE_GALLERY = 100;

    private ImageView profileImage;
    private TextView tvEmail, tvRole, tvCreationDate;
    private EditText etName;
    private Button btnSave, btnChangePassword;
    private MaterialToolbar toolbar;
    private ProgressBar progressBar;

    private Uri selectedImageUri;
    private String currentProfileImageUrl; // Store current profile image URL

    private MaterialButton changeProfileImage;

    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d(TAG, "onCreate started");
            setContentView(R.layout.activity_profile);
            Log.d(TAG, "Layout set successfully");

            // Initialisation Firebase
            initializeFirebase();

            if (currentUser == null) {
                Log.e(TAG, "Current user is null");
                Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d(TAG, "Current user: " + currentUser.getEmail());

            // Initialize views
            if (initializeViews()) {
                Log.d(TAG, "Views initialized successfully");
                loadUserProfile();
            } else {
                Log.e(TAG, "Failed to initialize views");
                Toast.makeText(this, "Erreur d'initialisation de l'interface", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Erreur lors du chargement: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeFirebase() {
        try {
            Log.d(TAG, "Initializing Firebase");
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            storage = FirebaseStorage.getInstance(); // Initialize Firebase Storage
            currentUser = mAuth.getCurrentUser();
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
            throw e;
        }
    }

    private boolean initializeViews() {
        try {
            Log.d(TAG, "Starting view initialization");

            // Find views with null checks
            profileImage = findViewById(R.id.profile_image);
            tvEmail = findViewById(R.id.tv_email);
            tvRole = findViewById(R.id.tv_role);
            tvCreationDate = findViewById(R.id.tv_creation_date);
            etName = findViewById(R.id.et_name);
            btnSave = findViewById(R.id.btn_save);
            btnChangePassword = findViewById(R.id.btn_change_password);
            toolbar = findViewById(R.id.toolbar);
            progressBar = findViewById(R.id.progressBar);
            changeProfileImage = findViewById(R.id.btn_change_photo);

            // Check for missing views
            if (profileImage == null) Log.w(TAG, "profile_image not found in layout");
            if (tvEmail == null) Log.w(TAG, "tv_email not found in layout");
            if (tvRole == null) Log.w(TAG, "tv_role not found in layout");
            if (tvCreationDate == null) Log.w(TAG, "tv_creation_date not found in layout");
            if (etName == null) Log.w(TAG, "et_name not found in layout");
            if (btnSave == null) Log.w(TAG, "btn_save not found in layout");
            if (btnChangePassword == null) Log.w(TAG, "btn_change_password not found in layout");
            if (toolbar == null) Log.w(TAG, "toolbar not found in layout");
            if (progressBar == null) Log.w(TAG, "progressBar not found in layout");

            // Setup image profile
            changeProfileImage.setOnClickListener(v -> openGallery());

            // Setup toolbar if available
            if (toolbar != null) {
                try {
                    setSupportActionBar(toolbar);
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    }
                    toolbar.setNavigationOnClickListener(v -> finish());
                    Log.d(TAG, "Toolbar setup successfully");
                } catch (Exception e) {
                    Log.e(TAG, "Error setting up toolbar", e);
                }
            }

            // Setup button listeners
            if (btnSave != null) {
                btnSave.setOnClickListener(v -> updateProfile());
                Log.d(TAG, "Save button listener set");
            }

            if (btnChangePassword != null) {
                btnChangePassword.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(this, ChangePasswordActivity.class));
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting ChangePasswordActivity", e);
                        Toast.makeText(this, "Impossible d'ouvrir la page de changement de mot de passe", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.d(TAG, "Change password button listener set");
            }

            // Hide progress bar initially
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            Log.d(TAG, "View initialization completed");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            return false;
        }
    }

    private void loadUserProfile() {
        try {
            String userEmail = currentUser.getEmail();

            if (userEmail == null || userEmail.isEmpty()) {
                Log.e(TAG, "User email is null or empty");
                Toast.makeText(this, "Erreur: email utilisateur non disponible", Toast.LENGTH_SHORT).show();
                displayBasicUserInfo();
                return;
            }

            Log.d(TAG, "Loading profile for email: " + userEmail);

            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }

            // Set up the document reference
            userRef = db.collection("utilisateurs").document(userEmail);

            // Load user data from Firestore
            userRef.get()
                    .addOnCompleteListener(task -> {
                        try {
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }

                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document != null && document.exists()) {
                                    Log.d(TAG, "Document found for email: " + userEmail);
                                    displayUserInfo(document);
                                } else {
                                    Log.w(TAG, "No document found for email: " + userEmail);
                                    displayBasicUserInfo();
                                    Toast.makeText(this, "Profil non trouvé dans la base de données", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.e(TAG, "Erreur lors du chargement du profil", task.getException());
                                displayBasicUserInfo();
                                Toast.makeText(this,
                                        "Erreur lors du chargement: " + (task.getException() != null ? task.getException().getMessage() : "Erreur inconnue"),
                                        Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error in loadUserProfile callback", e);
                            displayBasicUserInfo();
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error in loadUserProfile", e);
            displayBasicUserInfo();
            Toast.makeText(this, "Erreur lors du chargement du profil", Toast.LENGTH_SHORT).show();
        }
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
        if (selectedImageUri != null && profileImage != null) {
            Glide.with(this)
                    .load(selectedImageUri)
                    .circleCrop()
                    .into(profileImage);
        }
    }

    private void displayUserInfo(DocumentSnapshot document) {
        try {
            // Display basic Firebase Auth info
            displayBasicUserInfo();

            // Display additional Firestore data
            if (document != null) {
                String role = document.getString("Role");
                if (tvRole != null) {
                    tvRole.setText(role != null ?
                            ("admin".equals(role) ? "Administrateur" : "Utilisateur") :
                            "Rôle non défini");
                }

                Object createdAt = document.get("createdAt");
                if (tvCreationDate != null) {
                    if (createdAt instanceof com.google.firebase.Timestamp) {
                        Date date = ((com.google.firebase.Timestamp) createdAt).toDate();
                        String formattedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
                        tvCreationDate.setText("Membre depuis " + formattedDate);
                    } else {
                        tvCreationDate.setText("Date de création non disponible");
                    }
                }

                // Load profile image from Firestore
                currentProfileImageUrl = document.getString("profileImageUrl");
                loadProfileImage();

                Log.d(TAG, "User profile loaded successfully");
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'affichage des données", e);
            if (tvRole != null) tvRole.setText("Erreur chargement rôle");
            if (tvCreationDate != null) tvCreationDate.setText("Erreur chargement date");
        }
    }

    private void loadProfileImage() {
        if (profileImage != null) {
            if (selectedImageUri != null) {
                // Show newly selected image
                Glide.with(this)
                        .load(selectedImageUri)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(profileImage);
            } else if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                // Show saved profile image from Firestore
                Glide.with(this)
                        .load(currentProfileImageUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(profileImage);
            } else {
                // Show default image
                profileImage.setImageResource(R.drawable.ic_profile);
            }
        }
    }

    private void displayBasicUserInfo() {
        try {
            if (currentUser == null) {
                Log.e(TAG, "Current user is null in displayBasicUserInfo");
                return;
            }

            // Display Firebase Auth information
            if (etName != null) {
                etName.setText(currentUser.getDisplayName() != null ?
                        currentUser.getDisplayName() : "Non défini");
            }

            if (tvEmail != null) {
                tvEmail.setText(currentUser.getEmail() != null ?
                        currentUser.getEmail() : "Email non disponible");
            }

            // Load profile image
            loadProfileImage();

            Log.d(TAG, "Basic user info displayed");

        } catch (Exception e) {
            Log.e(TAG, "Error in displayBasicUserInfo", e);
        }
    }

    private void uploadImageToFirebaseStorage(Uri imageUri, String userId) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        StorageReference storageRef = storage.getReference();
        StorageReference profileImagesRef = storageRef.child("profile_images/" + userId + ".jpg");

        UploadTask uploadTask = profileImagesRef.putFile(imageUri);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Get download URL
            profileImagesRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Log.d(TAG, "Image uploaded successfully: " + downloadUrl);

                // Save URL to Firestore
                saveImageUrlToFirestore(downloadUrl);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get download URL", e);
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                Toast.makeText(this, "Erreur lors de l'obtention de l'URL de l'image", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Image upload failed", e);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            Toast.makeText(this, "Échec du téléchargement de l'image", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveImageUrlToFirestore(String imageUrl) {
        if (userRef != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("profileImageUrl", imageUrl);

            userRef.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Profile image URL saved to Firestore");
                        currentProfileImageUrl = imageUrl;
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        Toast.makeText(this, "Image de profil mise à jour avec succès", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save image URL to Firestore", e);
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        Toast.makeText(this, "Erreur lors de la sauvegarde de l'image", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateProfile() {
        try {
            if (etName == null) {
                Toast.makeText(this, "Erreur: champ nom non trouvé", Toast.LENGTH_SHORT).show();
                return;
            }

            String newName = etName.getText().toString().trim();
            if (newName.isEmpty()) {
                etName.setError("Nom requis");
                return;
            }

            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }

            // First, upload image if a new one was selected
            if (selectedImageUri != null) {
                uploadImageToFirebaseStorage(selectedImageUri, currentUser.getUid());
                selectedImageUri = null; // Reset after upload
            }

            // Update Firebase Auth profile
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();

            currentUser.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        try {
                            if (selectedImageUri == null && progressBar != null) {
                                // Only hide progress bar if not uploading image
                                progressBar.setVisibility(View.GONE);
                            }

                            if (task.isSuccessful()) {
                                Log.d(TAG, "Profile updated successfully");
                                if (selectedImageUri == null) {
                                    // Only show success message if not uploading image
                                    Toast.makeText(this, "Profil mis à jour avec succès", Toast.LENGTH_SHORT).show();
                                }
                                if (etName != null) {
                                    etName.setText(newName);
                                }
                            } else {
                                Log.e(TAG, "Erreur lors de la mise à jour", task.getException());
                                Toast.makeText(this,
                                        "Échec mise à jour: " + (task.getException() != null ? task.getException().getMessage() : "Erreur inconnue"),
                                        Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error in updateProfile callback", e);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error in updateProfile", e);
            Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            Log.d(TAG, "onResume called");
            // Refresh data if necessary
            if (currentUser != null) {
                currentUser.reload().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase user reloaded");
                        loadUserProfile();
                    } else {
                        Log.e(TAG, "Erreur lors du rechargement utilisateur", task.getException());
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }
}