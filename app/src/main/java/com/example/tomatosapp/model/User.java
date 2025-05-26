package com.example.tomatosapp.model;

import com.google.firebase.Timestamp;

import java.util.Objects;

public class User {
    private String userId;
    private String email;
    private String nom;
    private String role;
    private Timestamp createdAt;
    private boolean emailVerified;

    public User() {
        // Required for Firestore
    }

    public User(String userId, String email, String nom, String role, Timestamp createdAt, boolean emailVerified) {
        this.userId = userId;
        this.email = email;
        this.nom = nom;
        this.role = role;
        this.createdAt = createdAt;
        this.emailVerified = emailVerified;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return emailVerified == user.emailVerified &&
                Objects.equals(userId, user.userId) &&
                Objects.equals(email, user.email) &&
                Objects.equals(nom, user.nom) &&
                Objects.equals(role, user.role) &&
                Objects.equals(createdAt, user.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email, nom, role, createdAt, emailVerified);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", nom='" + nom + '\'' +
                ", role='" + role + '\'' +
                ", createdAt=" + createdAt +
                ", emailVerified=" + emailVerified +
                '}';
    }
}