package org.wordpress.android.login;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.store.SiteStore;

import java.util.ArrayList;

public interface LoginListener {
    interface SelfSignedSSLCallback {
        void certificateTrusted();
    }

    LoginMode getLoginMode();

    // Login Email input callbacks
    void gotWpcomEmail(String email, boolean verifyEmail);
    void loginViaSiteAddress();
    void loginViaSocialAccount(String email, String idToken, String service, boolean isPasswordRequired);
    void loggedInViaSocialAccount(ArrayList<Integer> oldSiteIds, boolean doLoginUpdate);
    void loginViaWpcomUsernameInstead();
    void loginViaSiteCredentials(String inputSiteAddress);
    void helpEmailScreen(String email);
    void helpSocialEmailScreen(String email);
    void addGoogleLoginFragment();
    void showHelpFindingConnectedEmail();

    // Login Request Magic Link callbacks
    void showMagicLinkSentScreen(String email);
    void usePasswordInstead(String email);
    void forgotPassword(String url);
    void helpMagicLinkRequest(String email);

    // Login Magic Link Sent callbacks
    void openEmailClient(boolean isLogin);
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
    void gotConnectedSiteInfo(@NonNull String siteAddress, @Nullable String redirectUrl, boolean hasJetpack);
    void gotXmlRpcEndpoint(String inputSiteAddress, String endpointAddress);
    void handleSslCertificateError(MemorizingTrustManager memorizingTrustManager, SelfSignedSSLCallback callback);
    void helpSiteAddress(String url);
    void helpFindingSiteAddress(String username, SiteStore siteStore);

    // Login username password callbacks
    void saveCredentialsInSmartLock(@Nullable String username, @Nullable String password,
                                    @NonNull String displayName, @Nullable Uri profilePicture);
    void loggedInViaUsernamePassword(ArrayList<Integer> oldSitesIds);
    void helpUsernamePassword(String url, String username, boolean isWpcom);
    void helpNoJetpackScreen(String siteAddress, String endpointAddress, String username,
                             String password, String userAvatarUrl, Boolean checkJetpackAvailability);
    void helpHandleDiscoveryError(String siteAddress, String endpointAddress, String username,
                                  String password, String userAvatarUrl, int errorMessage);

    // Login 2FA screen callbacks
    void help2FaScreen(String email);

    // General post-login callbacks
    // TODO This should have a more generic name, it more or less means any kind of login was finished
    void startPostLoginServices();

    // Signup
    void doStartSignup();
    void helpSignupEmailScreen(String email);
    void helpSignupMagicLinkScreen(String email);
    void showSignupMagicLink(String email);
    void showSignupToLoginMessage();
}
