package org.wordpress.android.ui.accounts.login;

public interface LoginListener {
    // Login Prologue callbacks
    void nextPromo();
    void showEmailLoginScreen();
    void doStartSignup();

    // Login Email input callbacks
    void showMagicLinkRequestScreen(String email);
    void loginViaSiteAddress();

    // Login Request Magic Link callbacks
    void showMagicLinkSentScreen(String email);
    void usePasswordInstead(String email);
    void forgotPassword();

    // Login Magic Link Sent callbacks
    void openEmailClient();

    // Login email password callbacks
    void needs2fa(String email, String password);
    void loggedInViaPassword();

    // Login Site Address input callbacks
    void gotWpcomSiteAddress();
    void gotXmlRpcEndpoint(String siteAddress);
    void helpWithSiteAddress();

    // Help callback
    void help();

    void setHelpContext(String faqId, String faqSection);
}
