package com.example.tomatosapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tomatosapp.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check authentication
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || !user.isEmailVerified()) {
            redirectToAuth();
            return;
        }

        // Initialize views
        TextView welcomeText = findViewById(R.id.welcomeText);
        Button startButton = findViewById(R.id.start_button);
        bottomNav = findViewById(R.id.bottom_navigation);

        // Set welcome message
        String userName = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                ? user.getDisplayName()
                : user.getEmail() != null
                ? user.getEmail()
                : "Guest";

        welcomeText.setText(getString(R.string.welcome_message, userName));

        // Logout button


        // Start button
        startButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ImageUploadActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // Setup bottom navigation
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_process) {
                    startActivity(new Intent(MainActivity.this, MainActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                    return true;
                }else if (id == R.id.nav_environment) {
                        startActivity(new Intent(MainActivity.this, EnvironmentActivity.class));
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        return true;

                } else if (id == R.id.nav_chatbot) {
                    startActivity(new Intent(MainActivity.this, ChatbotActivity.class));
                    return true;
                } else if (id == R.id.nav_logout) {
                    startActivity(new Intent(MainActivity.this, LogoutActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    return true;
                }
                return false;
            }
        });
    }

    private void redirectToAuth() {
        Intent intent = new Intent(this, AuthentificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || !user.isEmailVerified()) {
            redirectToAuth();
        }
    }
}