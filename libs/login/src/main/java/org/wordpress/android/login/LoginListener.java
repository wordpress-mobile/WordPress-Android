package org.wordpress.android.login;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload;

import java.util.ArrayList;
import java.util.List;

public interface LoginListener {
    interface SelfSignedSSLCallback {
        void certificateTrusted();
    }

    LoginMode getLoginMode();
    void startOver();

    // Login Email input callbacks
    void gotWpcomEmail(String email, boolean verifyEmail, @Nullable AuthOptions authOptions);
    void gotUnregisteredEmail(String email);
    void loginViaSiteAddress();
    void loginViaSiteCredentials(String inputSiteAddress);
    void helpEmailScreen(String email);
    void showHelpFindingConnectedEmail();
    void onTermsOfServiceClicked();

    // Login Magic Link Sent callbacks (used by SignupMagicLinkFragment)
    void openEmailClient(boolean isLogin);

    // Login 2FA callbacks
    void needs2fa(String email, String password);
    void needs2fa(String email, String password, String userId, String webauthnNonce,
                  String nonceAuthenticator, String nonceBackup, String noncePush,
                  List<String> supportedAuthTypes);
    void loggedInViaPassword(ArrayList<Integer> oldSitesIds);

    // Login Site Address input callbacks
    void alreadyLoggedInWpcom(ArrayList<Integer> oldSitesIds);
    void gotWpcomSiteInfo(String siteAddress);
    void gotConnectedSiteInfo(@NonNull String siteAddress, @Nullable String redirectUrl, boolean hasJetpack);
    void handleSslCertificateError(MemorizingTrustManager memorizingTrustManager, SelfSignedSSLCallback callback);
    void helpSiteAddress(String url);
    void helpFindingSiteAddress(String username, SiteStore siteStore);
    void handleSiteAddressError(ConnectSiteInfoPayload siteInfo);

    // Login 2FA screen callbacks
    void help2FaScreen(String email);

    // General post-login callbacks
    // TODO This should have a more generic name, it more or less means any kind of login was finished
    void startPostLoginServices();

    // Signup
    void helpSignupEmailScreen(String email);
    void helpSignupMagicLinkScreen(String email);
    void showSignupMagicLink(String email);
    void showSignupToLoginMessage();
}
