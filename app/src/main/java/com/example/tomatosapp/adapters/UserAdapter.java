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
import com.google.android.material.chip.Chip;

public class UserAdapter extends FirestoreRecyclerAdapter<User, UserAdapter.UserHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private final OnUserClickListener listener;

    public UserAdapter(@NonNull FirestoreRecyclerOptions<User> options, OnUserClickListener listener) {
        super(options);
        this.listener = listener;
    }

    @Override
    protected void onBindViewHolder(@NonNull UserHolder holder, int position, @NonNull User user) {
        holder.bind(user);
    }

    @NonNull
    @Override
    public UserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserHolder(view);
    }

    class UserHolder extends RecyclerView.ViewHolder {
        private final ImageView avatar;
        private final TextView email;
        private final TextView role;
        private final Chip statusChip;

        public UserHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.user_avatar);
            email = itemView.findViewById(R.id.user_email);
            role = itemView.findViewById(R.id.user_role);
            statusChip = itemView.findViewById(R.id.user_status_chip);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUserClick(getItem(position));
                }
            });
        }

        public void bind(User user) {
            email.setText(user.getEmail());

            // Gestion robuste du rôle
            String userRole = user.getRole();
            if (userRole == null) {
                userRole = "user";
            } else {
                userRole = userRole.toLowerCase().trim();
            }

            String displayRole;
            int chipColor;

            if (userRole.equals("admin")) {
                displayRole = "Administrateur";
                chipColor = R.color.admin_primary;
            } else {
                displayRole = "Utilisateur";
                chipColor = R.color.admin_secondary;
            }

            role.setText("Rôle: " + displayRole);
            statusChip.setText(displayRole);
            statusChip.setChipBackgroundColorResource(chipColor);
        }
    }
}