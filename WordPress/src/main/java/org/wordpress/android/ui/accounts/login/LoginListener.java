package org.wordpress.android.ui.accounts.login;

import org.wordpress.android.ui.accounts.LoginMode;
import org.wordpress.android.ui.accounts.SmartLockHelper;

import java.util.ArrayList;

public interface LoginListener {
    LoginMode getLoginMode();

    // Login Prologue callbacks
    void showEmailLoginScreen();
    void doStartSignup();
    void loggedInViaSignup(ArrayList<Integer> oldSitesIds);
    void newUserCreatedButErrored(String email, String password);

    // Login Email input callbacks
    void gotWpcomEmail(String email);
    void loginViaSiteAddress();
    void loginViaSocialAccount(String email, String idToken, String service, boolean isPasswordRequired);
    void loggedInViaSocialAccount(ArrayList<Integer> oldSiteIds);
    void loginViaWpcomUsernameInstead();
    void helpEmailScreen(String email);
    void helpSocialEmailScreen(String email);

    // Login Request Magic Link callbacks
    void showMagicLinkSentScreen(String email);
    void usePasswordInstead(String email);
    void forgotPassword(String url);
    void helpMagicLinkRequest(String email);

    // Login Magic Link Sent callbacks
    void openEmailClient();
    void helpMagicLinkSent(String email);

    // Login email password callbacks
    void needs2fa(String email, String password);
    void needs2faSocial(String email, String userId, String nonceAuthenticator, String nonceBackup, String nonceSms);
    void needs2faSocialConnect(String email, String password, String idToken, String service);
    void loggedInViaPassword(ArrayList<Integer> oldSitesIds);
    void helpEmailPasswordScreen(String email);

    // Login Site Address input callbacks
    void alreadyLoggedInWpcom(ArrayList<Integer> oldSitesIds);
    void gotWpcomSiteInfo(String siteAddress, String siteName, String siteIconUrl);
    void gotXmlRpcEndpoint(String inputSiteAddress, String endpointAddress);
    void helpSiteAddress(String url);

    // Login username password callbacks
    SmartLockHelper getSmartLockHelper();
    void loggedInViaUsernamePassword(ArrayList<Integer> oldSitesIds);
    void helpUsernamePassword(String url, String username, boolean isWpcom);

    // Login 2FA screen callbacks
    void help2FaScreen(String email);

    void setHelpContext(String faqId, String faqSection);
}
