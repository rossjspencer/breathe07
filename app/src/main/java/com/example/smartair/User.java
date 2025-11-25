package com.example.smartair;

public class User {
    public String email;
    public String role;
    public boolean onboardingComplete;

    public User() {
        // Required by Firebase
    }

    public User(String email, String role) {
        this.email = email;
        this.role = role;
        this.onboardingComplete = false; // first time user -> no onboarding yet
    }
}



