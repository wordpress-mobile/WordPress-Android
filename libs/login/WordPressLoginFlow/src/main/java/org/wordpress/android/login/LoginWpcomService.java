package org.wordpress.android.login;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnSocialChanged;
import org.wordpress.android.fluxc.store.AccountStore.PushSocialLoginPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;
import org.wordpress.android.login.LoginWpcomService.OnLoginStateUpdated;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutoForeground;
import org.wordpress.android.util.ToastUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class LoginWpcomService extends AutoForeground<OnLoginStateUpdated> {
    private static final String ARG_EMAIL = "ARG_EMAIL";
    private static final String ARG_PASSWORD = "ARG_PASSWORD";
    private static final String ARG_SOCIAL_ID_TOKEN = "ARG_SOCIAL_ID_TOKEN";
    private static final String ARG_SOCIAL_LOGIN = "ARG_SOCIAL_LOGIN";
    private static final String ARG_SOCIAL_SERVICE = "ARG_SOCIAL_SERVICE";

    public enum LoginPhase {
        IDLE,
        AUTHENTICATING(25),
        SOCIAL_LOGIN(25),
        FETCHING_ACCOUNT(50),
        FETCHING_SETTINGS(75),
        FETCHING_SITES(100),
        SUCCESS,
        FAILURE_EMAIL_WRONG_PASSWORD,
        FAILURE_2FA,
        FAILURE_SOCIAL_2FA,
        FAILURE_FETCHING_ACCOUNT,
        FAILURE_CANNOT_ADD_DUPLICATE_SITE,
        FAILURE;

        public final int progressPercent;

        LoginPhase() {
            this.progressPercent = 0;
        }

        LoginPhase(int progressPercent) {
            this.progressPercent = progressPercent;
        }

        public boolean isInProgress() {
            return this != LoginPhase.IDLE && !isTerminal();
        }

        public boolean isError() {
            return this == LoginPhase.FAILURE
                    || this == LoginPhase.FAILURE_EMAIL_WRONG_PASSWORD
                    || this == LoginPhase.FAILURE_2FA
                    || this == LoginPhase.FAILURE_SOCIAL_2FA
                    || this == LoginPhase.FAILURE_FETCHING_ACCOUNT
                    || this == LoginPhase.FAILURE_CANNOT_ADD_DUPLICATE_SITE;
        }

        public boolean isTerminal() {
            return this == LoginPhase.SUCCESS || isError();
        }
    }

    public static class OnLoginStateUpdated {
        public final LoginPhase state;

        OnLoginStateUpdated(LoginPhase state) {
            this.state = state;
        }
    }

    static class OnCredentialsOK {
        OnCredentialsOK() {}
    }

    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    private LoginPhase mLoginPhase = LoginPhase.IDLE;

    private String mIdToken;
    private String mService;
    private boolean mIsSocialLogin;

    public static void loginWithEmailAndPassword(
            Context context,
            String email,
            String password,
            String idToken, String service,
            boolean isSocialLogin) {
        Intent intent = new Intent(context, LoginWpcomService.class);
        intent.putExtra(ARG_EMAIL, email);
        intent.putExtra(ARG_PASSWORD, password);
        intent.putExtra(ARG_SOCIAL_ID_TOKEN, idToken);
        intent.putExtra(ARG_SOCIAL_SERVICE, service);
        intent.putExtra(ARG_SOCIAL_LOGIN, isSocialLogin);
        context.startService(intent);
    }

    public static void clearLoginServiceState() {
        EventBus.getDefault().removeStickyEvent(OnLoginStateUpdated.class);
    }

    public LoginWpcomService() {
        super(OnLoginStateUpdated.class);
    }

    @Override
    protected OnLoginStateUpdated getCurrentStateEvent() {
        return new OnLoginStateUpdated(mLoginPhase);
    }

    @Override
    public boolean isIdle() {
        return mLoginPhase == LoginPhase.IDLE;
    }

    @Override
    public boolean isInProgress() {
        return mLoginPhase.isInProgress();
    }

    @Override
    public boolean isError() {
        return mLoginPhase.isError();
    }

    @Override
    public Notification getNotification() {
        switch (mLoginPhase) {
            case AUTHENTICATING:
            case FETCHING_ACCOUNT:
            case FETCHING_SETTINGS:
            case FETCHING_SITES:
                return LoginNotification.progress(this, mLoginPhase.progressPercent, R.string.notification_logging_in);
            case SUCCESS:
                return LoginNotification.success(this, R.string.notification_logged_in);
            case FAILURE_EMAIL_WRONG_PASSWORD:
                return LoginNotification.failure(this, R.string.notification_error_wrong_password);
            case FAILURE_2FA:
                return LoginNotification.failure(this, R.string.notification_2fa_needed);
            case FAILURE_SOCIAL_2FA:
                return LoginNotification.failure(this, R.string.notification_2fa_needed);
            case FAILURE:
                return LoginNotification.failure(this, R.string.notification_login_failed);
        }

        return null;
    }

    private void setState(LoginPhase loginPhase) {
        if (!mLoginPhase.isInProgress() && loginPhase.isInProgress()) {
            mDispatcher.register(this);
        }

        mLoginPhase = loginPhase;
        track();
        notifyState();

        if (mLoginPhase.isTerminal()) {
            mDispatcher.unregister(this);
            stopSelf();
        }
    }

    private void track() {
        Map<String, Object> props = new HashMap<>();
        props.put("login_phase", mLoginPhase == null ? "null" : mLoginPhase.name());
        props.put("login_service_is_foreground", isForeground());
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_WPCOM_BACKGROUND_SERVICE_UPDATE, props);
    }

    @Override
    public void onCreate() {
        AndroidInjection.inject(this);
        super.onCreate();

        AppLog.i(T.MAIN, "LoginWpcomService > Created");

        // TODO: Recover any login attempts that were interrupted by the service being stopped?
    }

    @Override
    public void onDestroy() {
        AppLog.i(T.MAIN, "LoginWpcomService > Destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        setState(LoginPhase.AUTHENTICATING);

        String email = intent.getStringExtra(ARG_EMAIL);
        String password = intent.getStringExtra(ARG_PASSWORD);

        mIdToken = intent.getStringExtra(ARG_SOCIAL_ID_TOKEN);
        mService = intent.getStringExtra(ARG_SOCIAL_SERVICE);
        mIsSocialLogin = intent.getBooleanExtra(ARG_SOCIAL_LOGIN, false);

        AuthenticatePayload payload = new AuthenticatePayload(email, password);
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        AppLog.i(T.NUX, "User tries to log in wpcom. Email: " + email);

        return START_REDELIVER_INTENT;
    }

    private void handleAuthError(AuthenticationErrorType error, String errorMessage) {
        if (error != AuthenticationErrorType.NEEDS_2FA) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FAILED, error.getClass().getSimpleName(),
                    error.toString(), errorMessage);

            if (mIsSocialLogin) {
                AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_FAILURE, error.getClass().getSimpleName(),
                        error.toString(), errorMessage);
            }
        }

        switch (error) {
            case INCORRECT_USERNAME_OR_PASSWORD:
            case NOT_AUTHENTICATED: // NOT_AUTHENTICATED is the generic error from XMLRPC response on first call.
                setState(LoginPhase.FAILURE_EMAIL_WRONG_PASSWORD);
                break;
            case NEEDS_2FA:
                // login credentials were correct anyway so, offer to save to SmartLock
                signalCredentialsOK();

                if (mIsSocialLogin) {
                    setState(LoginPhase.FAILURE_SOCIAL_2FA);
                } else {
                    setState(LoginPhase.FAILURE_2FA);
                }

                break;
            case INVALID_REQUEST:
                // TODO: FluxC: could be specific?
            default:
                setState(LoginPhase.FAILURE);
                AppLog.e(T.NUX, "Server response: " + errorMessage);

                ToastUtils.showToast(this, errorMessage == null ? getString(R.string.error_generic) : errorMessage);
                break;
        }
    }

    private void fetchAccount() {
        setState(LoginPhase.FETCHING_ACCOUNT);
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
    }

    private void signalCredentialsOK() {
        EventBus.getDefault().post(new OnCredentialsOK());
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            AppLog.e(T.API, "onAuthenticationChanged has error: " + event.error.type + " - " + event.error.message);
            handleAuthError(event.error.type, event.error.message);
            return;
        }

        AppLog.i(T.NUX, "onAuthenticationChanged: " + event.toString());

        if (mIsSocialLogin) {
            setState(LoginPhase.SOCIAL_LOGIN);
            PushSocialLoginPayload payload = new PushSocialLoginPayload(mIdToken, mService);
            mDispatcher.dispatch(AccountActionBuilder.newPushSocialConnectAction(payload));
        } else {
            signalCredentialsOK();
            fetchAccount();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocialChanged(OnSocialChanged event) {
        if (event.isError()) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_CONNECT_FAILURE);
            switch (event.error.type) {
                case UNABLE_CONNECT:
                    AppLog.e(T.API, "Unable to connect WordPress.com account to social account.");
                    break;
                case USER_ALREADY_ASSOCIATED:
                    AppLog.e(T.API, "This social account is already associated with a WordPress.com account.");
                    break;
                // Ignore other error cases.  The above are the only two we have chosen to log.
            }

            fetchAccount();
        } else if (!event.requiresTwoStepAuth) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_CONNECT_SUCCESS);
            fetchAccount();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (event.isError()) {
            AppLog.e(T.API, "onAccountChanged has error: " + event.error.type + " - " + event.error.message);
            setState(LoginPhase.FAILURE_FETCHING_ACCOUNT);
            return;
        }

        if (event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
            setState(LoginPhase.FETCHING_SETTINGS);
            // The user's account info has been fetched and stored - next, fetch the user's settings
            mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
            setState(LoginPhase.FETCHING_SITES);
            // The user's account settings have also been fetched and stored - now we can fetch the user's sites
            mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        if (event.isError()) {
            AppLog.e(T.API, "onSiteChanged has error: " + event.error.type + " - " + event.error.toString());
            if (event.error.type != SiteErrorType.DUPLICATE_SITE) {
                setState(LoginPhase.FAILURE);
                return;
            }

            if (event.rowsAffected == 0) {
                // If there is a duplicate site and not any site has been added, show an error and
                // stop the sign in process
                setState(LoginPhase.FAILURE_CANNOT_ADD_DUPLICATE_SITE);
                return;
            } else {
                // If there is a duplicate site, notify the user something could be wrong,
                // but continue the sign in process
                ToastUtils.showToast(this, R.string.duplicate_site_detected);
            }
        }

        setState(LoginPhase.SUCCESS);
    }
}
