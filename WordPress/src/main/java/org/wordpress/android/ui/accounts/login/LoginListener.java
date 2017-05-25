package org.wordpress.android.ui.accounts.login;

public interface LoginListener {
    // Login Prologue callbacks
    void showEmailLoginScreen();
    void doStartSignup();

    // Login Email input callbacks
    void gotEmail(String email);
    void loginViaUsernamePassword();

    // Login Site Address input callbacks
    void gotSiteAddress(String siteAddress);

    void help();
}
