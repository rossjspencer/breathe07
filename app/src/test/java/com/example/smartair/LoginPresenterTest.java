package com.example.smartair;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@RunWith(MockitoJUnitRunner.class)
public class LoginPresenterTest {

    @Mock
    private LoginContract.View view;

    @Mock
    private LoginContract.Model model;

    private LoginPresenter presenter;

    @Before
    public void setUp() {
        presenter = new LoginPresenter(view, model);
    }

    // ---TEST 1: INPUT VALIDATION---
    @Test
    public void handleLogin_EmptyInput_ShowsInputError() {
        // Behavior: If fields are empty, view should show error and model should NOT be called
        presenter.handleLogin("", "");
        verify(view).showInputError();
        verify(model, never()).performParentLogin(any(), any(), any());
        verify(model, never()).performChildLogin(any(), any(), any());
    }

    // ---TEST 2: ROUTING LOGIC (Has @ vs No @)---
    @Test
    public void handleLogin_InputWithAtSymbol_CallsParentLogin() {
        // Behavior: Input has '@', so it should call performParentLogin
        String email = "parent@test.com";
        String pass = "password";
        presenter.handleLogin(email, pass);
        verify(model).performParentLogin(eq(email), eq(pass), eq(presenter));
    }

    @Test
    public void handleLogin_InputWithoutAtSymbol_CallsChildLogin() {
        // Behavior: Input has NO '@', so it should call performChildLogin
        String username = "childUser";
        String pass = "password";
        presenter.handleLogin(username, pass);
        verify(model).performChildLogin(eq(username), eq(pass), eq(presenter));
    }

    // ---TEST 3: SUCCESS CALLBACKS---
    @Test
    public void onParentSuccess_RoleParent_NavigatesToParentHome() {
        // Simulate model returning "Parent" role
        presenter.onParentSuccess("Parent");
        verify(view).navigateToParentHome();
    }

    @Test
    public void onParentSuccess_RoleProvider_NavigatesToProviderHome() {
        // Simulate model returning "Provider" role
        presenter.onParentSuccess("Provider");
        verify(view).navigateToProviderHome();
    }

    @Test
    public void onChildSuccess_NavigatesToChildHome() {
        String childId = "123";
        String name = "Sam";
        presenter.onChildSuccess(childId, name);

        verify(view).showLoginSuccess("Welcome back, " + name);
        verify(view).navigateToChildHome(childId);
    }

    // ---TEST 4: FAILURE HANDLING---
    @Test
    public void onFailure_ShowsLoginError() {
        String errorMsg = "Network Error";
        presenter.onFailure(errorMsg);
        verify(view).showLoginError(errorMsg);
    }

    @Test
    public void onParentSuccess_UnknownRole_ShowsError() {
        presenter.onParentSuccess("Hacker");
        verify(view).showLoginError("Unknown role: Hacker");
    }

    // Input Validation Tests
    @Test
    public void handleLogin_NullInput_ShowsInputError() {
        presenter.handleLogin(null, "password");
        verify(view).showInputError();
        verify(model, never()).performParentLogin(any(), any(), any());
        verify(model, never()).performChildLogin(any(), any(), any());
    }

    @Test
    public void handleLogin_NullPassword_ShowsInputError() {
        presenter.handleLogin("user", null);
        verify(view).showInputError();
    }

    @Test
    public void handleLogin_InputSpacesOnly_ShowsInputError() {
        presenter.handleLogin("   ", "password");
        verify(view).showInputError();
    }

    @Test
    public void handleLogin_PasswordSpacesOnly_ShowsInputError() {
        presenter.handleLogin("childUser", "   ");
        verify(view).showInputError();
    }

    @Test
    public void handleLogin_BothSpaces_ShowsInputError() {
        presenter.handleLogin("   ", "   ");
        verify(view).showInputError();
    }

    //Trimming Behavior
    @Test
    public void handleLogin_InputWithSpaces_StillRoutesCorrectly() {
        presenter.handleLogin("   parent@test.com   ", "  pass123  ");

        verify(model).performParentLogin(eq("parent@test.com"), eq("pass123"), eq(presenter));
    }

    @Test
    public void handleLogin_ChildWithSpaces_CallsChildLogin() {
        presenter.handleLogin("   kid123   ", " pass ");
        verify(model).performChildLogin(eq("kid123"), eq("pass"), eq(presenter));
    }

    //Roles Edge Cases
    @Test
    public void onParentSuccess_NullRole_ShowsUnknownRoleError() {
        presenter.onParentSuccess(null);
        verify(view).showLoginError("Unknown role: null");
    }

    @Test
    public void onParentSuccess_EmptyRole_ShowsUnknownRoleError() {
        presenter.onParentSuccess("");
        verify(view).showLoginError("Unknown role: ");
    }

    @Test
    public void onParentSuccess_LowercaseParent_ShowsUnknownRoleError() {
        presenter.onParentSuccess("parent");
        verify(view).showLoginError("Unknown role: parent");
    }

    //Special Characters / Weird Inputs
    @Test
    public void handleLogin_WeirdEmailStillCountsAsParent() {
        presenter.handleLogin("@@@@", "pass");
        verify(model).performParentLogin(eq("@@@@"), eq("pass"), eq(presenter));
    }

    @Test
    public void handleLogin_ChildNumericUsername_StillChild() {
        presenter.handleLogin("12345", "pass");
        verify(model).performChildLogin(eq("12345"), eq("pass"), eq(presenter));
    }

    //Failure Path Variants
    @Test
    public void onFailure_EmptyMessage_ShowsLoginError() {
        presenter.onFailure("");
        verify(view).showLoginError("");
    }

    @Test
    public void onFailure_NullMessage_ShowsLoginError() {
        presenter.onFailure(null);
        verify(view).showLoginError(null);
    }

    //Behavior Separation
    @Test
    public void handleLogin_PasswordContainsAtButInputDecidesParent() {
        presenter.handleLogin("parent@test.com", "p@ssword");
        verify(model).performParentLogin(eq("parent@test.com"), eq("p@ssword"), eq(presenter));
    }
}