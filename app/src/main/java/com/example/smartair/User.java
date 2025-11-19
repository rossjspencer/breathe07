package com.example.smartair;

import java.util.Map;
import java.util.HashMap;


public class User {
    public String userId;
    public String email;
    public String firstName;
    public String lastName;
    public String password;
    public String role;
    public String pairingCode;
    public Map<String, Boolean> linkedChildren = new HashMap<>();
    public int asthmaScore=100;

    public User() {
    }
    public User(String userId, String role, String firstName, String email) {
        this.userId = userId;
        this.role = role;
        this.firstName = firstName;
        this.email = email;
    }
}
