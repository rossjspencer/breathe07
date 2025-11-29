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
    public String dateOfBirth;
    public String notes;

    public Map<String, Boolean> linkedChildren = new HashMap<>();
    public int asthmaScore = 100;
    public int personalBest = 400;

    // Sharing Settings Node (nested object)
    public Map<String, Boolean> sharingSettings = new HashMap<>();

    public User() {}

    // Updated constructor to handle generic creation
    public User(String userId, String role, String email) {
        this.userId = userId;
        this.role = role;
        this.email = email;
    }
}