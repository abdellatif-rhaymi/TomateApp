package com.example.tomatosapp.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tomatosapp.R;
import com.example.tomatosapp.model.UserFeedback;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class FeedbackAdapter extends FirestoreRecyclerAdapter<UserFeedback, FeedbackAdapter.FeedbackHolder> {

    private static final String TAG = "FeedbackAdapter";

    public interface OnFeedbackClickListener {
        void onFeedbackClick(UserFeedback feedback, int position);
    }

    private final OnFeedbackClickListener listener;

    public FeedbackAdapter(@NonNull FirestoreRecyclerOptions<UserFeedback> options, OnFeedbackClickListener listener) {
        super(options);
        this.listener = listener;
    }

    @Override
    protected void onBindViewHolder(@NonNull FeedbackHolder holder, int position, @NonNull UserFeedback model) {
        try {
            // Vérifier que la position est valide
            if (position < 0 || position >= getItemCount()) {
                Log.w(TAG, "Invalid position: " + position + ", item count: " + getItemCount());
                return;
            }

            if (model != null) {
                holder.bind(model, position);
            } else {
                Log.w(TAG, "Null model at position: " + position);
                holder.bindEmpty();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error binding view at position " + position, e);
            holder.bindEmpty();
        }
    }

    @NonNull
    @Override
    public FeedbackHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feedback, parent, false);
        return new FeedbackHolder(view);
    }

    @Override
    public int getItemCount() {
        try {
            int count = super.getItemCount();
            Log.d(TAG, "Item count: " + count);
            return count;
        } catch (Exception e) {
            Log.e(TAG, "Error getting item count", e);
            // Ne pas retourner 0, retourner le count du parent ou gérer l'erreur différemment
            return super.getItemCount();
        }
    }

    class FeedbackHolder extends RecyclerView.ViewHolder {
        private final TextView userEmailView;
        private final RatingBar ratingBar;
        private final TextView messageView;
        private final TextView dateView;
        private final MaterialCardView cardView;
        private final Chip statusChip;
        private final View btnResolve;
        private final View btnDelete;


        public FeedbackHolder(View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            userEmailView = itemView.findViewById(R.id.user_email);
            ratingBar = itemView.findViewById(R.id.rating_bar);
            messageView = itemView.findViewById(R.id.feedback_message);
            dateView = itemView.findViewById(R.id.timestamp);
            statusChip = itemView.findViewById(R.id.status_chip);
            btnResolve = itemView.findViewById(R.id.btn_resolve);
            btnDelete = itemView.findViewById(R.id.btn_delete);


            itemView.setOnClickListener(v -> handleClick());
        }

        private void handleClick() {
            int position = getBindingAdapterPosition();
            Log.d(TAG, "Click at position: " + position);

            if (position == RecyclerView.NO_POSITION) {
                Log.w(TAG, "Click ignored: NO_POSITION");
                return;
            }

            if (listener == null) {
                Log.w(TAG, "Click ignored: no listener");
                return;
            }

            try {
                // Double vérification de la position
                if (position >= 0 && position < getItemCount()) {
                    UserFeedback feedback = getItem(position);
                    if (feedback != null) {
                        listener.onFeedbackClick(feedback, position);
                    } else {
                        Log.w(TAG, "Click ignored: null feedback at position " + position);
                    }
                } else {
                    Log.w(TAG, "Click ignored: invalid position " + position + ", count: " + getItemCount());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling click at position " + position, e);
            }
        }

        public void bind(UserFeedback feedback, int position) {
            try {
                if (feedback == null) {
                    bindEmpty();
                    return;
                }

                // Vérifier que tous les champs ne sont pas null
                userEmailView.setText(feedback.getUserEmail() != null ?
                        feedback.getUserEmail() : "Email non disponible");

                ratingBar.setRating(feedback.getRating());

                messageView.setText(feedback.getMessage() != null ?
                        feedback.getMessage() : "Pas de message");

                if (feedback.getTimestamp() != null) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                        dateView.setText(sdf.format(feedback.getTimestamp().toDate()));
                    } catch (Exception e) {
                        Log.e(TAG, "Error formatting date", e);
                        dateView.setText("Date non disponible");
                    }
                } else {
                    dateView.setText("Date non disponible");
                }

                // Update status chip
                if (feedback.isResolved()) {
                    statusChip.setText("Résolu");
                    statusChip.setChipBackgroundColorResource(R.color.status_resolved);
                } else {
                    statusChip.setText("En attente");
                    statusChip.setChipBackgroundColorResource(R.color.status_pending);
                }

                // Update button visibility/text based on status
                btnResolve.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);

            } catch (Exception e) {
                Log.e(TAG, "Error in bind method for position " + position, e);
                bindEmpty();
            }
        }

        public void bindEmpty() {
            // Affichage par défaut en cas d'erreur
            userEmailView.setText("Erreur de chargement");
            ratingBar.setRating(0);
            messageView.setText("");
            dateView.setText("");
            cardView.setCardBackgroundColor(0xFFFFFFFF);
            itemView.setVisibility(View.VISIBLE);
            statusChip.setText("Erreur");
            statusChip.setChipBackgroundColorResource(R.color.status_error);
        }
    }

    // Méthodes utiles pour le debugging
    @Override
    public void onDataChanged() {
        super.onDataChanged();
        Log.d(TAG, "Data changed, new count: " + getItemCount());
    }


    public void onError(@NonNull Exception e) {
        Log.e(TAG, "Firestore error", e);
    }
}