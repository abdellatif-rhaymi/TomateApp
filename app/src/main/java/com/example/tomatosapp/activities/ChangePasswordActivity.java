package com.example.tomatosapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tomatosapp.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private Button btnUpdatePassword;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Initialisation Firebase
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        // Initialisation des vues
        etCurrentPassword = findViewById(R.id.et_current_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnUpdatePassword = findViewById(R.id.btn_update_password);
        progressBar = findViewById(R.id.progressBar);

        btnUpdatePassword.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation
        if (currentPassword.isEmpty()) {
            etCurrentPassword.setError("Mot de passe actuel requis");
            etCurrentPassword.requestFocus();
            return;
        }

        if (newPassword.isEmpty() || newPassword.length() < 6) {
            etNewPassword.setError("Nouveau mot de passe (min 6 caractères)");
            etNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Les mots de passe ne correspondent pas");
            etConfirmPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnUpdatePassword.setEnabled(false);

        if (user != null && user.getEmail() != null) {
            // Recréer les credentials pour vérifier le mot de passe actuel
            AuthCredential credential = EmailAuthProvider
                    .getCredential(user.getEmail(), currentPassword);

            // Réauthentification
            user.reauthenticate(credential)
                    .addOnCompleteListener(authTask -> {
                        if (authTask.isSuccessful()) {
                            // Mise à jour du mot de passe
                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(updateTask -> {
                                        progressBar.setVisibility(View.GONE);
                                        btnUpdatePassword.setEnabled(true);

                                        if (updateTask.isSuccessful()) {
                                            Toast.makeText(ChangePasswordActivity.this,
                                                    "Mot de passe mis à jour avec succès",
                                                    Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            Toast.makeText(ChangePasswordActivity.this,
                                                    "Erreur: " + updateTask.getException().getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            progressBar.setVisibility(View.GONE);
                            btnUpdatePassword.setEnabled(true);
                            etCurrentPassword.setError("Mot de passe actuel incorrect");
                            etCurrentPassword.requestFocus();
                        }
                    });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right);
    }
}