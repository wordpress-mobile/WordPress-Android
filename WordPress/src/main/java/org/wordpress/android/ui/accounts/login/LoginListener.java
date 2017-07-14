package org.wordpress.android.ui.accounts.login;

import org.wordpress.android.ui.accounts.LoginMode;

public interface LoginListener {
    LoginMode getLoginMode();

    // Login Prologue callbacks
    void showEmailLoginScreen();
    void doStartSignup();
    void loggedInViaSigUp();
    void newUserCreatedButErrored(String email, String password);

    // Login Email input callbacks
    void gotWpcomEmail(String email);
    void loginViaSiteAddress();
    void loginViaWpcomUsernameInstead();

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
    void alreadyLoggedInWpcom();
    void gotWpcomSiteInfo(String siteAddress, String siteName, String siteIconUrl);
    void gotXmlRpcEndpoint(String inputSiteAddress, String endpointAddress);
    void helpWithSiteAddress();

    // Login username password callbacks
    void loggedInViaUsernamePassword();

    // Help callback
    void help();

    void setHelpContext(String faqId, String faqSection);
}
