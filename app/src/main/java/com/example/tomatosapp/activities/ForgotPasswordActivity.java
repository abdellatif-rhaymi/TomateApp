package com.example.tomatosapp.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tomatosapp.R;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailEditText;
    private Button resetPasswordButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    private Button btnBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialisation Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialisation des vues
        emailEditText = findViewById(R.id.email);
        resetPasswordButton = findViewById(R.id.btn_reset_password);
        progressBar = findViewById(R.id.progressBar);
        btnBackToLogin = findViewById(R.id.btn_back_to_login);

        resetPasswordButton.setOnClickListener(v -> resetPassword());
        btnBackToLogin.setOnClickListener(v -> backToLogin());
    }


    private void backToLogin(){

        startActivity(new Intent(this,AuthentificationActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void resetPassword() {
        String email = emailEditText.getText().toString().trim();

        // Validation de l'email
        if (email.isEmpty()) {
            emailEditText.setError("L'email est requis");
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Veuillez entrer un email valide");
            emailEditText.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        resetPasswordButton.setEnabled(false);

        // Envoi de l'email de réinitialisation
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    resetPasswordButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Vérifiez votre email pour réinitialiser votre mot de passe",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Erreur: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

}