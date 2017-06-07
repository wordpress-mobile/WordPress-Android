package org.wordpress.android.ui.accounts.login;

public interface LoginListener {
    // Login Prologue callbacks
    void nextPromo();
    void showEmailLoginScreen();
    void doStartSignup();

    // Login Email input callbacks
    void showMagicLinkRequestScreen(String email);
    void loginViaUsernamePassword();

    // Login Request Magic Link callbacks
    void showMagicLinkSentScreen(String email);
    void usePasswordInstead(String email);
    void forgotPassword();

    // Login Magic Link Sent callbacks
    void openEmailClient();

    // Login email password callbacks
    void loggedInViaPassword();

    // Login Site Address input callbacks
    void gotSiteAddress(String siteAddress);

    // Help callback
    void help();
}
