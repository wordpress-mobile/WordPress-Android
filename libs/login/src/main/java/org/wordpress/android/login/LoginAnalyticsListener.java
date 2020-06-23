package org.wordpress.android.login;

import java.util.Map;

public interface LoginAnalyticsListener {
    void trackAnalyticsSignIn(boolean isWpcomLogin);
    void trackCreatedAccount(String username, String email);
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
    void trackSignupMagicLinkOpenEmailClientViewed();
    void trackLoginMagicLinkOpenEmailClientViewed();
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
    void trackSocialButtonStart();
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
    void trackConnectedSiteInfoRequested(String url);
    void trackConnectedSiteInfoFailed(String url, String errorContext, String errorType, String errorDescription);
    void trackConnectedSiteInfoSucceeded(Map<String, ?> properties);
    void trackFailure(String message);
    void trackSendCodeWithTextClicked();

    void trackSubmit2faCodeClicked();

    void trackClickOnLoginSiteClicked();

    void trackSubmitClicked();

    void trackRequestMagicLinkClick();

    void trackLoginWithPasswordClick();

    void trackShowHelpClick();

    void trackDismissDialog();

    void trackEmailFieldClick();
}
