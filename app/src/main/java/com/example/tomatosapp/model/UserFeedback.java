package com.example.tomatosapp.model;


public class UserFeedback {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String message;
    private int rating;
    private com.google.firebase.Timestamp timestamp;
    private String userEmail;
    private String userId;
    private boolean resolved = false; // valeur par défaut

    // Constructeur vide requis pour Firestore
    public UserFeedback() {}

    // Getters et setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public com.google.firebase.Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(com.google.firebase.Timestamp timestamp) { this.timestamp = timestamp; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
}