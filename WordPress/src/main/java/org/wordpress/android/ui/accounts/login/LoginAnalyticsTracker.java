package org.wordpress.android.ui.accounts.login;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.login.LoginAnalyticsListener;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Flow;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.util.Map;

import javax.inject.Singleton;

@Singleton
public class LoginAnalyticsTracker implements LoginAnalyticsListener {
    private AccountStore mAccountStore;
    private SiteStore mSiteStore;
    private UnifiedLoginTracker mUnifiedLoginTracker;

    public LoginAnalyticsTracker(AccountStore accountStore, SiteStore siteStore, UnifiedLoginTracker unifiedLoginTracker) {
        this.mAccountStore = accountStore;
        this.mSiteStore = siteStore;
        mUnifiedLoginTracker = unifiedLoginTracker;
    }

    @Override
    public void trackAnalyticsSignIn(boolean isWpcom) {
        AnalyticsUtils.trackAnalyticsSignIn(mAccountStore, mSiteStore, isWpcom);
    }

    @Override
    public void trackCreatedAccount(String username, String email) {
        AnalyticsUtils.trackAnalyticsAccountCreated(username, email);
    }

    @Override
    public void trackEmailFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_EMAIL_FORM_VIEWED);
        mUnifiedLoginTracker.track(Flow.GET_STARTED, Step.START);
    }

    @Override
    public void trackInsertedInvalidUrl() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_INSERTED_INVALID_URL);
    }

    @Override
    public void trackLoginAccessed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_ACCESSED);
    }

    @Override
    public void trackLoginAutofillCredentialsFilled() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_AUTOFILL_CREDENTIALS_FILLED);
    }

    @Override
    public void trackLoginAutofillCredentialsUpdated() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_AUTOFILL_CREDENTIALS_UPDATED);
    }

    @Override
    public void trackLoginFailed(String errorContext, String errorType, String errorDescription) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FAILED, errorContext, errorType, errorDescription);
    }

    @Override
    public void trackLoginForgotPasswordClicked() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FORGOT_PASSWORD_CLICKED);
    }

    @Override
    public void trackLoginMagicLinkExited() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_EXITED);
    }

    @Override
    public void trackLoginMagicLinkOpened() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPENED);
    }

    @Override
    public void trackLoginMagicLinkOpenEmailClientClicked() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED);
        mUnifiedLoginTracker.track(Flow.LOGIN_MAGIC_LINK, Step.EMAIL_OPENED);
    }

    @Override
    public void trackLoginMagicLinkSucceeded() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_SUCCEEDED);
    }

    @Override
    public void trackLoginSocial2faNeeded() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_2FA_NEEDED);
    }

    @Override
    public void trackLoginSocialSuccess() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_SUCCESS);
    }

    @Override
    public void trackMagicLinkFailed(Map<String, ?> properties) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_FAILED, properties);
    }

    @Override
    public void trackSignupMagicLinkOpenEmailClientViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_VIEWED);
        mUnifiedLoginTracker.track(Flow.SIGNUP, Step.MAGIC_LINK_REQUESTED);
    }

    @Override
    public void trackLoginMagicLinkOpenEmailClientViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_VIEWED);
        mUnifiedLoginTracker.track(Flow.LOGIN_MAGIC_LINK, Step.MAGIC_LINK_REQUESTED);
    }

    @Override
    public void trackMagicLinkRequested() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_REQUESTED);
    }

    @Override
    public void trackMagicLinkRequestFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_REQUEST_FORM_VIEWED);
        mUnifiedLoginTracker.track(Flow.LOGIN_MAGIC_LINK, Step.START);
    }

    @Override
    public void trackPasswordFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_PASSWORD_FORM_VIEWED);
        mUnifiedLoginTracker.track(Flow.LOGIN_PASSWORD, Step.START);
    }

    @Override
    public void trackSignupCanceled() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_CANCELED);
    }

    @Override
    public void trackSignupEmailButtonTapped() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_EMAIL_BUTTON_TAPPED);
    }

    @Override
    public void trackSignupEmailToLogin() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_EMAIL_TO_LOGIN);
    }

    @Override
    public void trackSignupGoogleButtonTapped() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_BUTTON_TAPPED);
    }

    @Override
    public void trackSignupMagicLinkFailed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_MAGIC_LINK_FAILED);
    }

    @Override
    public void trackSignupMagicLinkOpened() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_MAGIC_LINK_OPENED);
    }

    @Override
    public void trackSignupMagicLinkOpenEmailClientClicked() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED);
        mUnifiedLoginTracker.track(Flow.SIGNUP, Step.EMAIL_OPENED);
    }

    @Override
    public void trackSignupMagicLinkSent() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_MAGIC_LINK_SENT);
    }

    @Override
    public void trackSignupMagicLinkSucceeded() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_MAGIC_LINK_SUCCEEDED);
    }

    @Override
    public void trackSignupSocialAccountsNeedConnecting() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_ACCOUNTS_NEED_CONNECTING);
    }

    @Override
    public void trackSignupSocialButtonFailure() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_BUTTON_FAILURE);
    }

    @Override
    public void trackSignupSocialToLogin() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_TO_LOGIN);
    }

    @Override
    public void trackSignupTermsOfServiceTapped() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_TERMS_OF_SERVICE_TAPPED);
    }

    @Override
    public void trackSocialButtonStart() {
        mUnifiedLoginTracker.track(Flow.LOGIN_SOCIAL, Step.START);
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
        mUnifiedLoginTracker.track(Step.TWO_FACTOR_AUTHENTICATION);
    }

    @Override
    public void trackUrlFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_URL_FORM_VIEWED);
        mUnifiedLoginTracker.track(Flow.LOGIN_SITE_ADDRESS, Step.START);
    }

    @Override
    public void trackUrlHelpScreenViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_URL_HELP_SCREEN_VIEWED);
        mUnifiedLoginTracker.track(Step.HELP);
    }

    @Override
    public void trackUsernamePasswordFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_USERNAME_PASSWORD_FORM_VIEWED);
        mUnifiedLoginTracker.track(Step.USERNAME_PASSWORD);
    }

    @Override
    public void trackWpComBackgroundServiceUpdate(Map<String, ?> properties) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_WPCOM_BACKGROUND_SERVICE_UPDATE, properties);
    }

    @Override public void trackConnectedSiteInfoRequested(String url) {
        // Not used in WordPress app
    }

    @Override
    public void trackConnectedSiteInfoFailed(String url, String errorContext, String errorType,
                                             String errorDescription) {
        // Not used in WordPress app
    }

    @Override public void trackConnectedSiteInfoSucceeded(Map<String, ?> properties) {
        // Not used in WordPress app
    }
}
