package com.example.tomatosapp.activities;

import android.content.Intent;
import android.os.Bundle;

import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tomatosapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LogoutActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logout);

        mAuth = FirebaseAuth.getInstance();

        Button btnCancel = findViewById(R.id.btn_cancel);
        Button btnLogout = findViewById(R.id.btn_logout);

        btnCancel.setOnClickListener(v -> {
            finish();
        });

        btnLogout.setOnClickListener(v -> {

            mAuth.signOut();

            Intent intent = new Intent(LogoutActivity.this, AuthentificationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finishAffinity();
        });
    }

}