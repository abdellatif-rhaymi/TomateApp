package com.example.tomatosapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
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

    private TextView adminUsers;
    private TextView totalUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        db = FirebaseFirestore.getInstance();
        usersRecyclerView = findViewById(R.id.users_recycler_view);
        progressBar = findViewById(R.id.progressBar);

        adminUsers = findViewById(R.id.admin_users);
        totalUsers = findViewById(R.id.total_users);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Load user counts
        loadUserCounts();

        setupRecyclerView();
    }

    private void loadUserCounts() {
        // Get total users count
        db.collection("utilisateurs")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int totalCount = task.getResult().size();
                        totalUsers.setText(String.valueOf(totalCount));

                        // Now get admin count
                        db.collection("utilisateurs")
                                .whereEqualTo("Role", "admin")
                                .get()
                                .addOnCompleteListener(adminTask -> {
                                    if (adminTask.isSuccessful()) {
                                        int adminCount = adminTask.getResult().size();
                                        adminUsers.setText(String.valueOf(adminCount));
                                    } else {
                                        Log.w(TAG, "Error getting admin count", adminTask.getException());
                                        adminUsers.setText("0");
                                    }
                                });
                    } else {
                        Log.w(TAG, "Error getting total user count", task.getException());
                        totalUsers.setText("0");
                        adminUsers.setText("0");
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

        userAdapter = new UserAdapter(options,
                user -> {
                    showUserDetails(user);
                },
                user -> {
                    deleteUser(user);
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

    private void deleteUser(User user) {
        // Show confirmation dialog before deleting
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Supprimer l'utilisateur")
                .setMessage("Êtes-vous sûr de vouloir supprimer cet utilisateur?\n\nEmail: " + user.getEmail())
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    performUserDeletion(user);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void performUserDeletion(User user) {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("utilisateurs")
                .document(user.getEmail())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Utilisateur supprimé avec succès", Toast.LENGTH_SHORT).show();
                    // Refresh user counts after deletion
                    loadUserCounts();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.w(TAG, "Error deleting user", e);
                    Toast.makeText(this, "Erreur lors de la suppression de l'utilisateur", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (userAdapter != null) {
            userAdapter.startListening();
            progressBar.setVisibility(View.GONE);
        }
        // Refresh counts when activity resumes
        loadUserCounts();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (userAdapter != null) {
            userAdapter.stopListening();
        }
    }
}