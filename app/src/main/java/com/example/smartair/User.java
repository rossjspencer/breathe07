package com.example.smartair;

import java.util.Map;
import java.util.HashMap;

public class User {
    public String userId;
    public String email;
    public String firstName;
    public String lastName;

    // NEW: Specific to Providers
    public String providerName;

    public String password;
    public String role;

    // ⭐ From your branch (keep this — important for onboarding flow)
    public boolean onboardingComplete = false;

    // ⭐ From main branch — keep all additional fields
    public String dateOfBirth;
    public String notes;

    public Map<String, Boolean> linkedChildren = new HashMap<>();
    public int asthmaScore = 100;
    public int personalBest = 400;

    // Sharing Settings Node (nested object)
    public Map<String, Boolean> sharingSettings = new HashMap<>();

    // Required empty constructor for Firebase
    public User() {}

    // Updated constructor to handle generic creation
    public User(String userId, String role, String email) {
        this.userId = userId;
        this.role = role;
        this.email = email;

        // Your feature preserved: new users start with onboarding incomplete
        this.onboardingComplete = false;
    }
}

