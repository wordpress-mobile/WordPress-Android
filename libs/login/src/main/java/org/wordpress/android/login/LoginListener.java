package org.wordpress.android.login;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
    void loginViaWpcomUsernameInstead();
    void helpEmailScreen(String email);

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
    void saveCredentials(@NonNull final String username, @NonNull final String password,
                         @NonNull final String displayName, @Nullable final Uri profilePicture);
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
