package com.example.smartair;

public class User {
    public String email;
    public String role;

    public User() {
        // required empty constructor
    }

    public User(String email, String role) {
        this.email = email;
        this.role = role;
    }
}
