package org.wordpress.android.ui.accounts.login;

public interface LoginListener {
    // Login Prologue callbacks
    void showEmailLoginScreen();
    void doStartSignup();

    // Login Email input callbacks
    void showMagicLinkRequestScreen(String email);
    void loginViaUsernamePassword();

    // Login Request Magic link callbacks
    void sendMagicLinkRequest(String email);
    void usePasswordInstead(String email);

    // Login Site Address input callbacks
    void gotSiteAddress(String siteAddress);

    // Help callback
    void help();
}
