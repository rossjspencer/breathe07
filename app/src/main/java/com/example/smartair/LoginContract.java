package com.example.smartair;

public interface LoginContract {

    // The View (Activity) handles UI only
    interface View {
        void showInputError();
        void showLoginError(String message);
        void showLoginSuccess(String message);

        // Home navigation
        void navigateToParentHome();
        void navigateToProviderHome();
        void navigateToChildHome(String childId);

        // Onboarding navigation
        void navigateToParentOnboarding();
        void navigateToProviderOnboarding();
        void navigateToChildOnboarding(String childId);
    }

    // The Presenter handles Logic
    interface Presenter {
        void handleLogin(String input, String password);
    }

    // The Model handles Data (Firebase)
    interface Model {

        interface OnLoginFinishedListener {
            // These currently exist, but we'll enhance them in the presenter
            void onParentSuccess(String role);
            void onChildSuccess(String childId, String firstName);
            void onFailure(String message);
        }

        void performParentLogin(String email, String password, OnLoginFinishedListener listener);
        void performChildLogin(String username, String password, OnLoginFinishedListener listener);
    }
}
