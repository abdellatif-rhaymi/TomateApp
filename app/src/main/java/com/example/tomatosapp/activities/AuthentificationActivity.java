package com.example.tomatosapp.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.tomatosapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthentificationActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView registerLink;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView forgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentification);

        // Initialisation Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Liaison des vues
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink); // Ajout du lien d'inscription
        forgotPassword = findViewById(R.id.forgotPassword);
        progressBar = findViewById(R.id.progressBar);

        // Gestion des clics
        loginButton.setOnClickListener(v -> loginUser());
        registerLink.setOnClickListener(v -> navigateToRegistration());
        forgotPassword.setOnClickListener(v-> navigateToForgotPassword());
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (!validateInputs(email, password)) return;

        showLoading(true);

        // Vérification de l'existence dans Firestore
        db.collection("utilisateurs").document(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            authenticateWithFirebase(email, password);
                        } else {
                            showLoading(false);
                            showErrorAndSuggestRegistration("Compte non trouvé. Voulez-vous créer un compte ?");
                        }
                    } else {
                        showLoading(false);
                        showError("Erreur de vérification");
                    }
                });
    }

    private void showErrorAndSuggestRegistration(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        // Vous pourriez aussi ajouter un bouton "S'inscrire" dans le Toast
        // ou une boîte de dialogue avec option d'inscription
    }

    private void navigateToRegistration() {
        startActivity(new Intent(this, RegistrationActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private boolean validateInputs(String email, String password) {
        if (email.isEmpty()) {
            emailInput.setError("Email requis");
            emailInput.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Format d'email invalide");
            emailInput.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Mot de passe requis");
            passwordInput.requestFocus();
            return false;
        }

        return true;
    }

    private void navigateToForgotPassword(){
        startActivity(new Intent(this, ForgotPasswordActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void authenticateWithFirebase(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkEmailVerification();
                    } else {
                        showLoading(false);
                        handleLoginError(task.getException());
                    }
                });
    }

    private void checkEmailVerification() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (user.isEmailVerified()) {
                fetchUserRoleAndRedirect(user.getEmail());
            } else {
                showLoading(false);
                showErrorAndResendOption("Email non vérifié. Vérifiez votre boîte mail.");
            }
        }
    }

    private void showErrorAndResendOption(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        // Option: Ajouter un bouton pour renvoyer l'email de vérification
    }

    private void fetchUserRoleAndRedirect(String email) {
        db.collection("utilisateurs").document(email)
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        String role = task.getResult().getString("Role");
                        redirectBasedOnRole(role);
                    } else {
                        showError("Erreur de récupération du rôle");
                    }
                });
    }

    private void redirectBasedOnRole(String role) {
        Intent intent;
        if ("admin".equals(role)) {
            intent = new Intent(this, AdminActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void handleLoginError(Exception exception) {
        String errorMessage = "Échec de la connexion";
        if (exception != null) {
            if (exception.getMessage().contains("invalid email")) {
                errorMessage = "Email invalide";
            } else if (exception.getMessage().contains("wrong password")) {
                errorMessage = "Mot de passe incorrect";
            } else if (exception.getMessage().contains("user not found")) {
                errorMessage = "Compte non trouvé. Voulez-vous créer un compte ?";
            }
        }
        showError(errorMessage);
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!isLoading);
        registerLink.setEnabled(!isLoading);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            fetchUserRoleAndRedirect(currentUser.getEmail());
        }
    }
}