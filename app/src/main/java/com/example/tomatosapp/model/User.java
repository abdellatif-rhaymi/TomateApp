package com.example.tomatosapp.model;

import com.google.firebase.Timestamp;

import java.util.Objects;

public class User {
    private String userId;
    private String email;
    private String Nom;
    private String Role;
    private Timestamp createdAt;
    private boolean emailVerified;

    public User() {
        // Required for Firestore
    }

    public User(String userId, String email, String Nom, String Role, Timestamp createdAt, boolean emailVerified) {
        this.userId = userId;
        this.email = email;
        this.Nom = Nom;
        this.Role = Role;
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
        return Nom;
    }

    public void setNom(String nom) {
        this.Nom = nom;
    }

    public String getRole() {
        return Role;
    }

    public void setRole(String Role) {
        this.Role = Role;
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
                Objects.equals(Nom, user.Nom) &&
                Objects.equals(Role, user.Role) &&
                Objects.equals(createdAt, user.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email, Nom, Role, createdAt, emailVerified);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", Nom='" + Nom + '\'' +
                ", role='" + Role + '\'' +
                ", createdAt=" + createdAt +
                ", emailVerified=" + emailVerified +
                '}';
    }
}