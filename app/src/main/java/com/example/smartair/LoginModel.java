package com.example.smartair;

import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginModel implements LoginContract.Model {

    private final FirebaseAuth auth;
    private final DatabaseReference db;

    public LoginModel() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseDatabase.getInstance().getReference("users");
    }

    // Constructor for testing (allows injecting mocks)
    public LoginModel(FirebaseAuth auth, DatabaseReference db) {
        this.auth = auth;
        this.db = db;
    }

    @Override
    public void performParentLogin(String email, String password, OnLoginFinishedListener listener) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = task.getResult().getUser().getUid();
                checkRole(uid, listener);
            } else {
                listener.onFailure(task.getException() != null ? task.getException().getMessage() : "Login failed");
            }
        });
    }

    @Override
    public void performChildLogin(String username, String password, OnLoginFinishedListener listener) {
        db.orderByChild("email").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    boolean matchFound = false;
                    for (DataSnapshot userSnap : snapshot.getChildren()) {
                        User user = userSnap.getValue(User.class);
                        if (user != null && user.password != null && user.password.equals(password)) {
                            listener.onChildSuccess(userSnap.getKey(), user.firstName);
                            matchFound = true;
                            break;
                        }
                    }
                    if (!matchFound) listener.onFailure("Incorrect Password");
                } else {
                    listener.onFailure("Username not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onFailure("Database Error: " + error.getMessage());
            }
        });
    }

    private void checkRole(String uid, OnLoginFinishedListener listener) {
        db.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    if (role != null) {
                        listener.onParentSuccess(role);
                    } else {
                        listener.onFailure("Role not found");
                    }
                } else {
                    listener.onFailure("User profile not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onFailure("Database Error");
            }
        });
    }
}