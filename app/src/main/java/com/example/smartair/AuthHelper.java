package com.example.smartair;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

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

    // Redirect user directly to their home screen based on role
    public static void redirectUser(Activity activity, String role) {
        Intent intent;

        if ("Parent".equals(role)) {
            intent = new Intent(activity, ParentHomeActivity.class);
        } else if ("Provider".equals(role)) {
            intent = new Intent(activity, ProviderHomeActivity.class);
        } else {
            Toast.makeText(activity, "Invalid role", Toast.LENGTH_SHORT).show();
            return;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void handlePostAuth(Activity activity, String uid) {
        getUserRef(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(activity, "User data missing", Toast.LENGTH_LONG).show();
                    return;
                }

                String role = snapshot.child("role").getValue(String.class);
                Boolean onboardingDone = snapshot.child("onboardingComplete").getValue(Boolean.class);

                if (role == null) {
                    Toast.makeText(activity, "Role missing", Toast.LENGTH_LONG).show();
                    return;
                }

                // First-time users -> show onboarding
                if (onboardingDone == null || !onboardingDone) {
                    Intent intent;
                    if ("Parent".equals(role)) {
                        intent = new Intent(activity, ParentOnboardingActivity.class);
                    } else { // Provider
                        intent = new Intent(activity, ProviderOnboardingActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(intent);
                    activity.finish();
                } else {
                    // Onboarding already done -> go to correct home
                    redirectUser(activity, role);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(activity, "Database error", Toast.LENGTH_LONG).show();
            }
        });
    }
}


