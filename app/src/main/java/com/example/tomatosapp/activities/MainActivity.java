package com.example.tomatosapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tomatosapp.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startButton = findViewById(R.id.start_button);

        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ImageUploadActivity.class);
            startActivity(intent);
        });
    }
}