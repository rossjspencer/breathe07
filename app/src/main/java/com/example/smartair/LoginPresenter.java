package com.example.smartair;

public class LoginPresenter implements LoginContract.Presenter, LoginContract.Model.OnLoginFinishedListener {

    private final LoginContract.View view;
    private final LoginContract.Model model;

    public LoginPresenter(LoginContract.View view, LoginContract.Model model) {
        this.view = view;
        this.model = model;
    }

    @Override
    public void handleLogin(String input, String password) {
        if (input == null || password == null || input.trim().isEmpty() || password.trim().isEmpty()) {
            view.showInputError();
            return;
        }

        // Logic from your requirements: '@' means Parent/Provider, else Child
        if (input.contains("@")) {
            model.performParentLogin(input.trim(), password.trim(), this);
        } else {
            model.performChildLogin(input.trim(), password.trim(), this);
        }
    }

    @Override
    public void onParentSuccess(String role) {
        if ("Parent".equals(role)) {
            view.navigateToParentHome();
        } else if ("Provider".equals(role)) {
            view.navigateToProviderHome();
        } else {
            view.showLoginError("Unknown role: " + role);
        }
    }

    @Override
    public void onChildSuccess(String childId, String firstName) {
        view.showLoginSuccess("Welcome back, " + firstName);
        view.navigateToChildHome(childId);
    }

    @Override
    public void onFailure(String message) {
        view.showLoginError(message);
    }
}