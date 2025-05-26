package com.example.tomatosapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tomatosapp.R;
import com.google.firebase.auth.FirebaseAuth;

public class AdminLogoutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logout_admin); // Remplacez par le nom de votre fichier XML

        // Initialisation des boutons
        Button btnLogout = findViewById(R.id.btn_logout_admin);
        Button btnCancel = findViewById(R.id.btn_cancel_admin);

        // Gestion du clic sur le bouton Déconnexion
        btnLogout.setOnClickListener(v -> {
            // Déconnexion de Firebase
            FirebaseAuth.getInstance().signOut();

            // Redirection vers l'écran de connexion
            Intent intent = new Intent(AdminLogoutActivity.this, AuthentificationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Ferme l'activité actuelle
        });

        // Gestion du clic sur le bouton Annuler
        btnCancel.setOnClickListener(v -> {
            finish(); // Ferme simplement le dialogue
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish(); // Pour fermer l'activité si l'utilisateur appuie sur retour
    }
}