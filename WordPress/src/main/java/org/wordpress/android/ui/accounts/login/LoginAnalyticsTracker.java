package org.wordpress.android.ui.accounts.login;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.login.LoginAnalyticsListener;
import org.wordpress.android.util.AnalyticsUtils;

import java.util.Map;

import javax.inject.Singleton;

@Singleton
public class LoginAnalyticsTracker implements LoginAnalyticsListener {
    @Override
    public void trackAnalyticsSignIn(AccountStore accountStore, SiteStore siteStore, boolean isWpcomLogin) {
        AnalyticsUtils.trackAnalyticsSignIn(accountStore, siteStore, isWpcomLogin);
    }

    @Override
    public void trackEmailFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_EMAIL_FORM_VIEWED);
    }

    @Override
    public void trackInsertedInvalidUrl() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_INSERTED_INVALID_URL);
    }

    @Override
    public void trackLoginFailed(String errorContext, String errorType, String errorDescription) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FAILED, errorContext, errorType, errorDescription);
    }

    @Override
    public void trackMagicLinkFailed(Map<String, ?> properties) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_FAILED, properties);
    }

    @Override
    public void trackMagicLinkOpenEmailClientViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_VIEWED);
    }

    @Override
    public void trackMagicLinkRequested() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_REQUESTED);
    }

    @Override
    public void trackMagicLinkRequestFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_REQUEST_FORM_VIEWED);
    }

    @Override
    public void trackPasswordFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_PASSWORD_FORM_VIEWED);
    }

    @Override
    public void trackSocialAccountsNeedConnecting() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_ACCOUNTS_NEED_CONNECTING);
    }

    @Override
    public void trackSocialButtonClick() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_BUTTON_CLICK);
    }

    @Override
    public void trackSocialButtonFailure() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_BUTTON_FAILURE);
    }

    @Override
    public void trackSocialConnectFailure() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_CONNECT_FAILURE);
    }

    @Override
    public void trackSocialConnectSuccess() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_CONNECT_SUCCESS);
    }

    @Override
    public void trackSocialErrorUnknownUser() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_ERROR_UNKNOWN_USER);
    }

    @Override
    public void trackSocialFailure(String errorContext, String errorType, String errorDescription) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_FAILURE, errorContext, errorType, errorDescription);
    }

    @Override
    public void trackTwoFactorFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_TWO_FACTOR_FORM_VIEWED);
    }

    @Override
    public void trackUrlFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_URL_FORM_VIEWED);
    }

    @Override
    public void trackUrlHelpScreenViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_URL_HELP_SCREEN_VIEWED);
    }

    @Override
    public void trackUsernamePasswordFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_USERNAME_PASSWORD_FORM_VIEWED);
    }

    @Override
    public void trackWpComBackgroundServiceUpdate(Map<String, ?> properties) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_WPCOM_BACKGROUND_SERVICE_UPDATE, properties);
    }
}
