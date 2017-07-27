package org.wordpress.android.ui.accounts.login;

import org.wordpress.android.ui.accounts.LoginMode;
import org.wordpress.android.ui.accounts.SmartLockHelper;

import java.util.ArrayList;

public interface LoginListener {
    LoginMode getLoginMode();

    // Login Prologue callbacks
    void showEmailLoginScreen();
    void doStartSignup();
    void loggedInViaSigUp(ArrayList<Integer> oldSitesIds);
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
    void loggedInViaPassword(ArrayList<Integer> oldSitesIds);

    // Login Site Address input callbacks
    void alreadyLoggedInWpcom(ArrayList<Integer> oldSitesIds);
    void gotWpcomSiteInfo(String siteAddress, String siteName, String siteIconUrl);
    void gotXmlRpcEndpoint(String inputSiteAddress, String endpointAddress);
    void helpWithSiteAddress();

    // Login username password callbacks
    SmartLockHelper getSmartLockHelper();
    void loggedInViaUsernamePassword(ArrayList<Integer> oldSitesIds);

    // Help callback
    void help();

    void setHelpContext(String faqId, String faqSection);
}
