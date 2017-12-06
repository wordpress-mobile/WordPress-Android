package org.wordpress.android.login;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;

import java.util.ArrayList;
import java.util.Map;

public interface LoginListener {
    interface SelfSignedSSLCallback {
        void certificateTrusted();
    }

    LoginMode getLoginMode();

    // Login Email input callbacks
    void gotWpcomEmail(String email);
    void loginViaSiteAddress();
    void loginViaSocialAccount(String email, String idToken, String service, boolean isPasswordRequired);
    void loggedInViaSocialAccount(ArrayList<Integer> oldSiteIds);
    void loginViaWpcomUsernameInstead();
    void helpEmailScreen(String email);
    void helpSocialEmailScreen(String email);
    void showGoogleLoginScreen(Fragment fragment);

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
    void handleSslCertificateError(MemorizingTrustManager memorizingTrustManager, SelfSignedSSLCallback callback);
    void helpSiteAddress(String url);
    void helpFindingSiteAddress(String username, SiteStore siteStore);

    // Login username password callbacks
    void saveCredentials(@Nullable String username, @Nullable String password,
                         @NonNull String displayName, @Nullable Uri profilePicture);
    void loggedInViaUsernamePassword(ArrayList<Integer> oldSitesIds);
    void helpUsernamePassword(String url, String username, boolean isWpcom);

    // Login 2FA screen callbacks
    void help2FaScreen(String email);

    // General post-login callbacks
    // TODO This should have a more generic name, it more or less means any kind of login was finished
    void startPostLoginServices();

    void setHelpContext(String faqId, String faqSection);

    // Analytics
    void track(AnalyticsTracker.Stat stat);
    void track(AnalyticsTracker.Stat stat, Map<String, ?> properties);
    void track(AnalyticsTracker.Stat stat, String errorContext, String errorType, String errorDescription);
    void trackAnalyticsSignIn(AccountStore accountStore, SiteStore siteStore, boolean isWpcomLogin);
}
