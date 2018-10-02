package org.wordpress.android.login;

import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;

import java.util.Map;

public interface LoginAnalyticsListener {
    void trackAnalyticsSignIn(AccountStore accountStore, SiteStore siteStore, boolean isWpcomLogin);
    void trackCreatedAccount();
    void trackEmailFormViewed();
    void trackInsertedInvalidUrl();
    void trackLoginAccessed();
    void trackLoginAutofillCredentialsFilled();
    void trackLoginAutofillCredentialsUpdated();
    void trackLoginFailed(String errorContext, String errorType, String errorDescription);
    void trackLoginForgotPasswordClicked();
    void trackLoginMagicLinkExited();
    void trackLoginMagicLinkOpened();
    void trackLoginMagicLinkOpenEmailClientClicked();
    void trackLoginMagicLinkSucceeded();
    void trackLoginSocial2faNeeded();
    void trackLoginSocialSuccess();
    void trackMagicLinkFailed(Map<String, ?> properties);
    void trackMagicLinkOpenEmailClientViewed();
    void trackMagicLinkRequested();
    void trackMagicLinkRequestFormViewed();
    void trackPasswordFormViewed();
    void trackSignupCanceled();
    void trackSignupEmailButtonTapped();
    void trackSignupEmailToLogin();
    void trackSignupGoogleButtonTapped();
    void trackSignupMagicLinkFailed();
    void trackSignupMagicLinkOpened();
    void trackSignupMagicLinkOpenEmailClientClicked();
    void trackSignupMagicLinkSent();
    void trackSignupMagicLinkSucceeded();
    void trackSignupSocialAccountsNeedConnecting();
    void trackSignupSocialButtonFailure();
    void trackSignupSocialToLogin();
    void trackSignupTermsOfServiceTapped();
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
