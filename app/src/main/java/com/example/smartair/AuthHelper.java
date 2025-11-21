package com.example.smartair;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AuthHelper {

    // Validate that email and password are not empty
    public static boolean validateCredentials(String email, String password) {
        return !(email.isEmpty() || password.isEmpty());
    }

    // Show toast message
    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    // Get database reference: users / uid
    public static DatabaseReference getUserRef(String uid) {
        return FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);
    }

    public static DatabaseReference getUsersRef() {
        return FirebaseDatabase.getInstance().getReference("users");
    }

    // Redirect user based on role
    public static void redirectUser(Context context, String role) {
        Intent intent = null;

        switch (role) {
            case "Parent":
                intent = new Intent(context, ParentHomeActivity.class);
                break;
            case "Provider":
                intent = new Intent(context, ProviderHomeActivity.class);
                break;
        }

        if (intent != null) {
            context.startActivity(intent);
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
        }
    }
}

