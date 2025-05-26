package com.example.tomatosapp.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.example.tomatosapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RegistrationActivity extends AppCompatActivity {

    private EditText editTextFullName, editTextEmail, editTextPassword, editTextConfirmPassword;
    private AutoCompleteTextView roleSpinner;
    private Button buttonRegister;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRoleSpinner();

        buttonRegister.setOnClickListener(v -> validateAndRegister());
        findViewById(R.id.login_text).setOnClickListener(v -> navigateToLogin());
    }

    private void initViews() {
        editTextFullName = findViewById(R.id.nom);
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        editTextConfirmPassword = findViewById(R.id.confirm_password);
        roleSpinner = findViewById(R.id.role_spinner);
        buttonRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRoleSpinner() {
        String[] rolesDisplay = getResources().getStringArray(R.array.roles_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                rolesDisplay
        );
        roleSpinner.setAdapter(adapter);
        roleSpinner.setText(rolesDisplay[0], false);
    }

    private void validateAndRegister() {
        String fullName = editTextFullName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (!validateInputs(fullName, email, password, confirmPassword)) {
            return;
        }

        String roleDisplay = roleSpinner.getText().toString();
        String[] rolesDisplay = getResources().getStringArray(R.array.roles_array);
        String[] rolesValues = getResources().getStringArray(R.array.roles_values);

        // Correction de la méthode pour obtenir l'index
        int roleIndex = Arrays.asList(rolesDisplay).indexOf(roleDisplay);
        String role = rolesValues[roleIndex];

        showLoading(true);
        checkEmailAndRegister(fullName, email, password, role);
    }

    private boolean validateInputs(String fullName, String email, String password, String confirmPassword) {
        // Validation du nom complet
        if (fullName.isEmpty()) {
            editTextFullName.setError("Le nom complet est requis");
            editTextFullName.requestFocus();
            return false;
        }

        if (fullName.length() < 3) {
            editTextFullName.setError("Le nom doit contenir au moins 3 caractères");
            editTextFullName.requestFocus();
            return false;
        }

        // Validation de l'email
        if (email.isEmpty()) {
            editTextEmail.setError("L'email est requis");
            editTextEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Veuillez entrer un email valide");
            editTextEmail.requestFocus();
            return false;
        }

        // Validation du mot de passe
        if (password.isEmpty()) {
            editTextPassword.setError("Le mot de passe est requis");
            editTextPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Le mot de passe doit contenir au moins 6 caractères");
            editTextPassword.requestFocus();
            return false;
        }

        // Validation de la confirmation du mot de passe
        if (confirmPassword.isEmpty()) {
            editTextConfirmPassword.setError("Veuillez confirmer votre mot de passe");
            editTextConfirmPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Les mots de passe ne correspondent pas");
            editTextConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void checkEmailAndRegister(String fullName, String email, String password, String role) {
        db.collection("utilisateurs").document(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            showLoading(false);
                            showError("Email déjà enregistré");
                        } else {
                            createFirebaseUser(fullName, email, password, role);
                        }
                    } else {
                        showLoading(false);
                        showError("Erreur de vérification");
                    }
                });
    }

    private void createFirebaseUser(String fullName, String email, String password, String role) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), fullName, email, password, role);
                        }
                    } else {
                        showLoading(false);
                        handleRegistrationError(task.getException());
                    }
                });
    }

    private void saveUserToFirestore(String userId, String fullName, String email,String password, String role) {
        Map<String, Object> user = new HashMap<>();
        user.put("Nom", fullName);
        user.put("email", email);
        user.put("motDePasse", password);
        user.put("Role", role);
        user.put("createdAt", FieldValue.serverTimestamp());
        user.put("userId", userId);

        db.collection("utilisateurs").document(email)
                .set(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        sendVerificationEmail();
                    } else {
                        showLoading(false);
                        showError("Erreur d'enregistrement");
                    }
                });
    }

    private void sendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            showSuccessAndNavigate();
                        } else {
                            showError("Échec d'envoi du email");
                        }
                    });
        }
    }

    private void handleRegistrationError(Exception exception) {
        String errorMessage = "Échec d'inscription";
        if (exception != null && exception.getMessage() != null) {
            if (exception.getMessage().contains("email address is already")) {
                errorMessage = "Email déjà utilisé";
            } else if (exception.getMessage().contains("password is invalid")) {
                errorMessage = "Mot de passe trop faible";
            }
        }
        showError(errorMessage);
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        buttonRegister.setEnabled(!isLoading);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showSuccessAndNavigate() {
        Toast.makeText(this, "Inscription réussie! Vérifiez votre email.", Toast.LENGTH_LONG).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, AuthentificationActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}