package com.example.tomatosapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tomatosapp.R;
import com.example.tomatosapp.adapters.FeedbackAdapter;
import com.example.tomatosapp.model.UserFeedback;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AdminActivity extends AppCompatActivity {

    private static final String TAG = "AdminActivity";
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FeedbackAdapter adapter;
    private FirebaseFirestore db;
    private BottomNavigationView bottomNav;
    private boolean isDataLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        Log.d(TAG, "AdminActivity created");

        initializeViews();
        setupRecyclerView();
        setupBottomNavigation();
    }

    private void initializeViews() {
        try {
            db = FirebaseFirestore.getInstance();
            recyclerView = findViewById(R.id.feedback_recycler_view);
            progressBar = findViewById(R.id.progressBar);
            bottomNav = findViewById(R.id.bottom_navigation);

            if (recyclerView == null) {
                Log.e(TAG, "RecyclerView not found in layout");
                showToast("Error: UI initialization failed");
                return;
            }

            // Configuration de base du RecyclerView
            recyclerView.setHasFixedSize(true);
            recyclerView.setItemAnimator(null); // Désactiver les animations pour éviter les conflits

            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            showToast("Error initializing interface");
        }
    }

    private void setupBottomNavigation() {
        if (bottomNav == null) {
            Log.e(TAG, "BottomNavigationView not found");
            return;
        }

        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Log.d(TAG, "Navigation item selected: " + id);

            try {
                if (id == R.id.nav_dashboard) {
                    refreshData();
                    return true;
                } else if (id == R.id.nav_profile_admin) {
                    navigateToActivity(ProfileActivity.class);
                    return true;
                } else if (id == R.id.nav_users) {
                    navigateToActivity(UserManagementActivity.class);
                    return true;
                } else if (id == R.id.nav_feedbacks) {
                    if (recyclerView != null) {
                        recyclerView.smoothScrollToPosition(0);
                    }
                    return true;
                } else if (id == R.id.nav_logout_admin) {
                    navigateToActivity(AdminLogoutActivity.class);
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling navigation", e);
                showToast("Navigation error");
            }
            return false;
        });
    }

    private void navigateToActivity(Class<?> activityClass) {
        try {
            Intent intent = new Intent(AdminActivity.this, activityClass);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to " + activityClass.getSimpleName(), e);
            showToast("Navigation error");
        }
    }

    private void refreshData() {
        if (isDataLoading) {
            Log.d(TAG, "Data already loading, ignoring refresh request");
            return;
        }

        Log.d(TAG, "Refreshing data");
        showProgressBar(true);
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        try {
            isDataLoading = true;

            // Arrêter l'adaptateur précédent proprement
            if (adapter != null) {
                Log.d(TAG, "Stopping previous adapter");
                adapter.stopListening();
                adapter = null;
            }

            // Créer la requête Firestore
            Query query = createFirestoreQuery();
            if (query == null) {
                Log.e(TAG, "Failed to create Firestore query");
                showProgressBar(false);
                isDataLoading = false;
                return;
            }

            // Créer les options pour l'adaptateur
            FirestoreRecyclerOptions<UserFeedback> options = new FirestoreRecyclerOptions.Builder<UserFeedback>()
                    .setQuery(query, UserFeedback.class)
                    .setLifecycleOwner(this) // Important pour la gestion automatique du lifecycle
                    .build();

            // Créer le nouvel adaptateur avec callbacks
            adapter = createFeedbackAdapter(options);

            // Configurer le RecyclerView
            if (recyclerView != null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setAdapter(adapter);
                Log.d(TAG, "RecyclerView configured with new adapter");
            } else {
                Log.e(TAG, "RecyclerView is null");
                showProgressBar(false);
                isDataLoading = false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
            showToast("Error loading feedback");
            showProgressBar(false);
            isDataLoading = false;
        }
    }

    private Query createFirestoreQuery() {
        try {
            // Utiliser 'feedbacks' selon votre structure Firebase
            Query query = db.collection("feedbacks")
                    .orderBy("timestamp", Query.Direction.DESCENDING);

            Log.d(TAG, "Firestore query created successfully");
            return query;
        } catch (Exception e) {
            Log.e(TAG, "Error creating Firestore query", e);
            return null;
        }
    }

    private FeedbackAdapter createFeedbackAdapter(FirestoreRecyclerOptions<UserFeedback> options) {
        return new FeedbackAdapter(options, this::showFeedbackDetails) {
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                Log.d(TAG, "Adapter data changed. Items: " + getItemCount());

                runOnUiThread(() -> {
                    showProgressBar(false);
                    isDataLoading = false;

                    if (getItemCount() == 0) {
                        showToast("Aucun feedback trouvé");
                    } else {
                        Log.d(TAG, "Loaded " + getItemCount() + " feedback items");
                    }
                });
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);
                Log.e(TAG, "Firestore adapter error", e);

                runOnUiThread(() -> {
                    showProgressBar(false);
                    isDataLoading = false;
                    showToast("Erreur lors du chargement des feedbacks");
                });
            }
        };
    }

    private void showFeedbackDetails(UserFeedback feedback, int position) {
        if (feedback == null) {
            Log.w(TAG, "Attempted to show details for null feedback at position: " + position);
            showToast("Erreur: feedback non disponible");
            return;
        }

        try {
            Log.d(TAG, "Showing feedback details for position: " + position);

            // Créer le contenu du dialogue
            StringBuilder details = new StringBuilder();
            details.append("De: ").append(feedback.getUserEmail() != null ?
                    feedback.getUserEmail() : "Email non disponible").append("\n\n");
            details.append("Note: ").append(feedback.getRating()).append("/5\n\n");
            details.append("Message: ").append(feedback.getMessage() != null ?
                    feedback.getMessage() : "Pas de message").append("\n\n");

            if (feedback.getTimestamp() != null) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.getDefault());
                    details.append("Date: ").append(sdf.format(feedback.getTimestamp().toDate())).append("\n\n");
                } catch (Exception e) {
                    Log.e(TAG, "Error formatting timestamp", e);
                    details.append("Date: Non disponible\n\n");
                }
            }

            details.append("Statut: ").append(feedback.isResolved() ? "Résolu" : "En attente");

            // Afficher le dialogue
            showFeedbackDialog(feedback, details.toString(), position);

        } catch (Exception e) {
            Log.e(TAG, "Error showing feedback details", e);
            showToast("Erreur lors de l'affichage des détails");
        }
    }

    private void showFeedbackDialog(UserFeedback feedback, String details, int position) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Détails du Feedback")
                    .setMessage(details)
                    .setPositiveButton("Fermer", (dialog, which) -> dialog.dismiss())
                    .setNegativeButton(feedback.isResolved() ? "Marquer non résolu" : "Marquer résolu",
                            (dialog, which) -> toggleFeedbackStatus(feedback, position))
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing feedback dialog", e);
            showToast("Erreur lors de l'affichage du dialogue");
        }
    }

    private void toggleFeedbackStatus(UserFeedback feedback, int position) {
        try {
            // Cette méthode nécessiterait l'implémentation de la mise à jour Firestore
            // Pour l'instant, on affiche juste un message
            String newStatus = feedback.isResolved() ? "non résolu" : "résolu";
            showToast("Fonctionnalité à implémenter: marquer comme " + newStatus);
            Log.d(TAG, "Toggle status requested for feedback at position: " + position);
        } catch (Exception e) {
            Log.e(TAG, "Error toggling feedback status", e);
            showToast("Erreur lors de la mise à jour du statut");
        }
    }

    private void showProgressBar(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showToast(String message) {
        try {
            runOnUiThread(() -> {
                Toast.makeText(AdminActivity.this, message, Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast: " + message, e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");

        // Le FirestoreRecyclerAdapter avec setLifecycleOwner gère automatiquement le lifecycle
        // Pas besoin d'appeler manuellement startListening()
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");

        // Le FirestoreRecyclerAdapter avec setLifecycleOwner gère automatiquement le lifecycle
        // Pas besoin d'appeler manuellement stopListening()
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");

        // Nettoyage final si nécessaire
        if (adapter != null && !isFinishing()) {
            try {
                adapter.stopListening();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping adapter in onDestroy", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");

        // Vérifier si nous devons rafraîchir les données
        if (adapter == null) {
            Log.d(TAG, "Adapter is null in onResume, setting up RecyclerView");
            setupRecyclerView();
        }
    }
}