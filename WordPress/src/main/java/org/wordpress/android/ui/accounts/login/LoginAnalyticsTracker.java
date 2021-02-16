package org.wordpress.android.ui.accounts.login;

import org.jetbrains.annotations.NotNull;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.login.LoginAnalyticsListener;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Click;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Flow;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

@Singleton
public class LoginAnalyticsTracker implements LoginAnalyticsListener {
    private AccountStore mAccountStore;
    private SiteStore mSiteStore;
    private UnifiedLoginTracker mUnifiedLoginTracker;

    public LoginAnalyticsTracker(AccountStore accountStore, SiteStore siteStore,
                                 UnifiedLoginTracker unifiedLoginTracker) {
        this.mAccountStore = accountStore;
        this.mSiteStore = siteStore;
        mUnifiedLoginTracker = unifiedLoginTracker;
    }

    @Override
    public void trackAnalyticsSignIn(boolean isWpcom) {
        AnalyticsUtils.trackAnalyticsSignIn(mAccountStore, mSiteStore, isWpcom);
    }

    @Override
    public void trackCreatedAccount(String username, String email, CreatedAccountSource source) {
        AnalyticsUtils.trackAnalyticsAccountCreated(username, email, source.asPropertyMap());
    }

    @Override
    public void trackEmailFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_EMAIL_FORM_VIEWED);
        mUnifiedLoginTracker.track(Flow.WORDPRESS_COM, Step.START);
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
        mUnifiedLoginTracker.track(Flow.SMART_LOCK_LOGIN, Step.START);
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
        mUnifiedLoginTracker.trackClick(Click.FORGOTTEN_PASSWORD);
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
    public void trackPasswordFormViewed(boolean isSocialChallenge) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_PASSWORD_FORM_VIEWED);
        if (isSocialChallenge) {
            mUnifiedLoginTracker.track(Flow.GOOGLE_LOGIN, Step.PASSWORD_CHALLENGE);
        } else {
            mUnifiedLoginTracker.track(Flow.LOGIN_PASSWORD, Step.START);
        }
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
        mUnifiedLoginTracker.track(Flow.GOOGLE_LOGIN, Step.START);
    }

    @Override
    public void trackSocialAccountsNeedConnecting() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_ACCOUNTS_NEED_CONNECTING);
    }

    @Override
    public void trackSocialButtonClick() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_BUTTON_CLICK);
        mUnifiedLoginTracker.trackClick(Click.LOGIN_WITH_GOOGLE);
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

    @Override
    public void trackConnectedSiteInfoRequested(String url) {
        Map<String, String> properties = new HashMap<>();
        properties.put("url", url);
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_CONNECTED_SITE_INFO_REQUESTED, properties);
    }

    @Override
    public void trackConnectedSiteInfoFailed(String url, String errorContext, String errorType,
                                             String errorDescription) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_CONNECTED_SITE_INFO_FAILED, errorContext, errorType,
                errorDescription);
    }

    @Override
    public void trackConnectedSiteInfoSucceeded(@NotNull Map<String, ?> properties) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_CONNECTED_SITE_INFO_SUCCEEDED, properties);
    }

    @Override
    public void trackFailure(String message) {
        mUnifiedLoginTracker.trackFailure(message);
    }

    @Override
    public void trackSendCodeWithTextClicked() {
        mUnifiedLoginTracker.trackClick(Click.SEND_CODE_WITH_TEXT);
    }

    @Override
    public void trackSubmit2faCodeClicked() {
        mUnifiedLoginTracker.trackClick(Click.SUBMIT_2FA_CODE);
    }

    @Override
    public void trackSubmitClicked() {
        mUnifiedLoginTracker.trackClick(Click.SUBMIT);
    }

    @Override
    public void trackRequestMagicLinkClick() {
        mUnifiedLoginTracker.trackClick(Click.REQUEST_MAGIC_LINK);
    }

    @Override
    public void trackLoginWithPasswordClick() {
        mUnifiedLoginTracker.trackClick(Click.LOGIN_WITH_PASSWORD);
    }

    @Override
    public void trackShowHelpClick() {
        mUnifiedLoginTracker.trackClick(Click.SHOW_HELP);
        mUnifiedLoginTracker.track(Step.HELP);
    }

    @Override
    public void trackDismissDialog() {
        mUnifiedLoginTracker.trackClick(Click.DISMISS);
    }

    @Override
    public void trackSelectEmailField() {
        mUnifiedLoginTracker.trackClick(Click.SELECT_EMAIL_FIELD);
    }

    @Override
    public void trackPickEmailFromHint() {
        mUnifiedLoginTracker.trackClick(Click.PICK_EMAIL_FROM_HINT);
    }

    @Override
    public void trackShowEmailHints() {
        mUnifiedLoginTracker.track(Step.SHOW_EMAIL_HINTS);
    }

    @Override
    public void emailFormScreenResumed() {
        mUnifiedLoginTracker.setFlowAndStep(Flow.WORDPRESS_COM, Step.START);
    }

    @Override
    public void trackSocialSignupConfirmationViewed() {
        mUnifiedLoginTracker.track(Flow.GOOGLE_SIGNUP, Step.START);
    }

    @Override
    public void trackCreateAccountClick() {
        mUnifiedLoginTracker.trackClick(Click.CREATE_ACCOUNT);
    }

    @Override public void emailPasswordFormScreenResumed() {
        mUnifiedLoginTracker.setStep(Step.START);
    }

    @Override public void siteAddressFormScreenResumed() {
        mUnifiedLoginTracker.setStep(Step.START);
    }

    @Override public void magicLinkRequestScreenResumed() {
        mUnifiedLoginTracker.setStep(Step.START);
    }

    @Override public void magicLinkSentScreenResumed() {
        mUnifiedLoginTracker.setStep(Step.MAGIC_LINK_REQUESTED);
    }

    @Override public void usernamePasswordScreenResumed() {
        mUnifiedLoginTracker.setStep(Step.USERNAME_PASSWORD);
    }
}
