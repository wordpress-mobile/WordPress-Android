package org.wordpress.android.login;

import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;

import java.util.Map;

public interface LoginAnalyticsListener {
    void trackAnalyticsSignIn(AccountStore accountStore, SiteStore siteStore, boolean isWpcomLogin);
    void trackEmailFormViewed();
    void trackInsertedInvalidUrl();
    void trackLoginFailed(String errorContext, String errorType, String errorDescription);
    void trackMagicLinkFailed(Map<String, ?> properties);
    void trackMagicLinkOpenEmailClientViewed();
    void trackMagicLinkRequested();
    void trackMagicLinkRequestFormViewed();
    void trackPasswordFormViewed();
    void trackSocialAccountsNeedConnecting();
    void trackSocialButtonClick();
    void trackSocialButtonFailure();
    void trackSocialConnectFailure();
    void trackSocialConnectSuccess();
    void trackSocialErrorUnknownUser();
    void trackSocialFailure(String errorContext, String errorType, String errorDescription);
    void trackTwoFactorFormViewed();
    void trackUrlFormViewed();
    void trackUrlHelpScreenViewed();
    void trackUsernamePasswordFormViewed();
    void trackWpComBackgroundServiceUpdate(Map<String, ?> properties);
}
