package com.example.smartair;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginPresenter implements LoginContract.Presenter, LoginContract.Model.OnLoginFinishedListener {

    private final LoginContract.View view;
    private final LoginContract.Model model;

    public LoginPresenter(LoginContract.View view, LoginContract.Model model) {
        this.view = view;
        this.model = model;
    }

    @Override
    public void handleLogin(String input, String password) {
        if (input.isEmpty() || password.isEmpty()) {
            view.showInputError();
            return;
        }

        // Child login → uses username
        if (!input.contains("@")) {
            model.performChildLogin(input, password, this);
        } else {
            // Parent/Provider login → uses email
            model.performParentLogin(input, password, this);
        }
    }

    @Override
    public void onParentSuccess(String role) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(uid);

        ref.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                view.showLoginError("Error loading profile");
                return;
            }

            DataSnapshot snap = task.getResult();
            Boolean done = snap.child("onboardingComplete").getValue(Boolean.class);
            if (done == null) done = false;

            // Route by role + onboarding state
            switch (role) {

                case "Parent":
                    if (!done) {
                        view.navigateToParentOnboarding();
                    } else {
                        view.navigateToParentHome();
                    }
                    break;

                case "Provider":
                    if (!done) {
                        view.navigateToProviderOnboarding();
                    } else {
                        view.navigateToProviderHome();
                    }
                    break;

                default:
                    view.showLoginError("Unknown role: " + role);
            }
        });
    }

    @Override
    public void onChildSuccess(String childId, String firstName) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(childId);

        ref.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                view.showLoginError("Error loading child data");
                return;
            }

            Boolean done = task.getResult().child("onboardingComplete").getValue(Boolean.class);
            if (done == null) done = false;

            if (!done) {
                view.navigateToChildOnboarding(childId);
            } else {
                view.navigateToChildHome(childId);
            }
        });
    }

    @Override
    public void onFailure(String message) {
        view.showLoginError(message);
    }
}
