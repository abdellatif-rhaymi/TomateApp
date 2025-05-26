package com.example.tomatosapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tomatosapp.R;
import com.example.tomatosapp.adapters.UserAdapter;
import com.example.tomatosapp.model.User;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class UserManagementActivity extends AppCompatActivity {

    private static final String TAG = "UserManagement";
    private RecyclerView usersRecyclerView;
    private ProgressBar progressBar;
    private UserAdapter userAdapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        db = FirebaseFirestore.getInstance();
        usersRecyclerView = findViewById(R.id.users_recycler_view);
        progressBar = findViewById(R.id.progressBar);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Debug: Vérifier les données Firestore
        debugFirestoreData();

        setupRecyclerView();
    }

    private void debugFirestoreData() {
        db.collection("utilisateurs")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Nombre d'utilisateurs: " + task.getResult().size());
                        task.getResult().forEach(doc -> {
                            Log.d(TAG, "ID: " + doc.getId() +
                                    " | Email: " + doc.getString("email") +
                                    " | Role: " + doc.getString("role"));
                        });
                    } else {
                        Log.w(TAG, "Erreur de chargement Firestore", task.getException());
                    }
                });
    }

    private void setupRecyclerView() {
        progressBar.setVisibility(View.VISIBLE);

        Query query = db.collection("utilisateurs")
                .orderBy("email", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
                .setQuery(query, User.class)
                .setLifecycleOwner(this)
                .build();

        userAdapter = new UserAdapter(options, user -> {
            showUserDetails(user);
        });

        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setAdapter(userAdapter);
    }

    private void showUserDetails(User user) {
        String role = user.getRole() != null ? user.getRole() : "non défini";
        Toast.makeText(this,
                "Email: " + user.getEmail() + "\n" +
                        "Rôle: " + role + "\n" +
                        "ID: " + user.getUserId(),
                Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (userAdapter != null) {
            userAdapter.startListening();
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (userAdapter != null) {
            userAdapter.stopListening();
        }
    }
}