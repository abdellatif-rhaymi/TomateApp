package com.example.tomatosapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tomatosapp.R;
import com.example.tomatosapp.model.User;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

public class UserAdapter extends FirestoreRecyclerAdapter<User, UserAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(User user);
    }

    private OnUserClickListener userClickListener;
    private OnDeleteClickListener deleteClickListener;

    public UserAdapter(@NonNull FirestoreRecyclerOptions<User> options,
                       OnUserClickListener userClickListener,
                       OnDeleteClickListener deleteClickListener) {
        super(options);
        this.userClickListener = userClickListener;
        this.deleteClickListener = deleteClickListener;
    }

    @Override
    protected void onBindViewHolder(@NonNull UserViewHolder holder, int position, @NonNull User user) {
        holder.bind(user);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        private ImageView userAvatar;
        private TextView userEmail;
        private TextView userRole;
        private Chip userStatusChip;
        private MaterialButton deleteButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.user_avatar);
            userEmail = itemView.findViewById(R.id.user_email);
            userRole = itemView.findViewById(R.id.user_role);
            userStatusChip = itemView.findViewById(R.id.user_status_chip);
            deleteButton = itemView.findViewById(R.id.btn_delete_user);
        }

        public void bind(User user) {
            // Set user email
            userEmail.setText(user.getEmail() != null ? user.getEmail() : "Email non défini");

            // Set user role
            String role = user.getRole() != null ? user.getRole() : "non défini";
            userRole.setText("Rôle: " + role);

            // Set status chip based on user activity or role
            updateStatusChip(user);

            // Configure delete button based on user role
            configureDeleteButton(user);

            // Set click listener for the entire item
            itemView.setOnClickListener(v -> {
                if (userClickListener != null) {
                    userClickListener.onUserClick(user);
                }
            });
        }

        private void updateStatusChip(User user) {
            String role = user.getRole() != null ? user.getRole().toLowerCase() : "utilisateur";

            if ("admin".equals(role)) {
                userStatusChip.setText("Admin");
                userStatusChip.setChipBackgroundColorResource(R.color.admin_accent);
            } else {
                userStatusChip.setText("Utilisateur");
                userStatusChip.setChipBackgroundColorResource(R.color.status_active);
            }
        }

        private void configureDeleteButton(User user) {
            String role = user.getRole() != null ? user.getRole().toLowerCase() : "utilisateur";

            if ("admin".equals(role)) {
                // Disable delete button for admin users
                deleteButton.setEnabled(false);
                deleteButton.setAlpha(0.5f);
                deleteButton.setOnClickListener(null);
            } else {
                // Enable delete button for regular users
                deleteButton.setEnabled(true);
                deleteButton.setAlpha(1.0f);
                deleteButton.setOnClickListener(v -> {
                    if (deleteClickListener != null) {
                        deleteClickListener.onDeleteClick(user);
                    }
                });
            }
        }
    }
}