package org.wordpress.android.login;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload;

import java.util.ArrayList;

public interface LoginListener {
    interface SelfSignedSSLCallback {
        void certificateTrusted();
    }

    LoginMode getLoginMode();
    void startOver();

    // Login navigation
    void loginViaSiteAddress();

    // Login Magic Link Sent callbacks (used by SignupMagicLinkFragment)
    void openEmailClient(boolean isLogin);

    // Login Site Address input callbacks
    void alreadyLoggedInWpcom(ArrayList<Integer> oldSitesIds);
    void gotConnectedSiteInfo(@NonNull String siteAddress, @Nullable String redirectUrl, boolean hasJetpack);
    void handleSslCertificateError(MemorizingTrustManager memorizingTrustManager, SelfSignedSSLCallback callback);
    void helpSiteAddress(String url);
    void helpFindingSiteAddress(String username, SiteStore siteStore);
    void handleSiteAddressError(ConnectSiteInfoPayload siteInfo);

    // General post-login callbacks
    void startPostLoginServices();

    // Signup
    void helpSignupMagicLinkScreen(String email);
    void showSignupMagicLink(String email);
    void showSignupToLoginMessage();
}
