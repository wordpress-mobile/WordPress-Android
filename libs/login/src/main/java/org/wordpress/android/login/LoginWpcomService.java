package org.wordpress.android.login;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnSocialChanged;
import org.wordpress.android.fluxc.store.AccountStore.PushSocialPayload;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;
import org.wordpress.android.login.LoginWpcomService.LoginState;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutoForeground;
import org.wordpress.android.util.AutoForegroundNotification;
import org.wordpress.android.util.ToastUtils;

import java.util.Map;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class LoginWpcomService extends AutoForeground<LoginState> {
    private static final String ARG_EMAIL = "ARG_EMAIL";
    private static final String ARG_PASSWORD = "ARG_PASSWORD";
    private static final String ARG_SOCIAL_ID_TOKEN = "ARG_SOCIAL_ID_TOKEN";
    private static final String ARG_SOCIAL_LOGIN = "ARG_SOCIAL_LOGIN";
    private static final String ARG_SOCIAL_SERVICE = "ARG_SOCIAL_SERVICE";

    public enum LoginStep {
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
        FAILURE_USE_WPCOM_USERNAME_INSTEAD_OF_EMAIL,
        FAILURE;

        public final int progressPercent;

        LoginStep() {
            this.progressPercent = 0;
        }

        LoginStep(int progressPercent) {
            this.progressPercent = progressPercent;
        }
    }

    public static class LoginState implements AutoForeground.ServiceState {
        private final LoginStep mStep;

        LoginState(@NonNull LoginStep step) {
            this.mStep = step;
        }

        LoginStep getStep() {
            return mStep;
        }

        @Override
        public boolean isIdle() {
            return mStep == LoginStep.IDLE;
        }

        @Override
        public boolean isInProgress() {
            return mStep != LoginStep.IDLE && !isTerminal();
        }

        @Override
        public boolean isError() {
            return mStep == LoginStep.FAILURE
                    || mStep == LoginStep.FAILURE_EMAIL_WRONG_PASSWORD
                    || mStep == LoginStep.FAILURE_2FA
                    || mStep == LoginStep.FAILURE_SOCIAL_2FA
                    || mStep == LoginStep.FAILURE_FETCHING_ACCOUNT
                    || mStep == LoginStep.FAILURE_CANNOT_ADD_DUPLICATE_SITE
                    || mStep == LoginStep.FAILURE_USE_WPCOM_USERNAME_INSTEAD_OF_EMAIL;
        }

        @Override
        public boolean isTerminal() {
            return mStep == LoginStep.SUCCESS || isError();
        }

        @Override
        public String getStepName() {
            return mStep.name();
        }
    }

    private static class LoginNotification {
        static Notification progress(Context context, int progress) {
            return AutoForegroundNotification.progress(context,
                    context.getString(R.string.login_notification_channel_id),
                    progress,
                    R.string.notification_login_title_in_progress,
                    R.string.notification_logging_in,
                    R.drawable.login_notification_icon,
                    R.color.login_notification_accent_color);
        }

        static Notification success(Context context) {
            return AutoForegroundNotification.success(context,
                    context.getString(R.string.login_notification_channel_id),
                    R.string.notification_login_title_success,
                    R.string.notification_logged_in,
                    R.drawable.login_notification_icon,
                    R.color.login_notification_accent_color);
        }

        static Notification failure(Context context, @StringRes int content) {
            return AutoForegroundNotification.failure(context,
                    context.getString(R.string.login_notification_channel_id),
                    R.string.notification_login_title_stopped,
                    content,
                    R.drawable.login_notification_icon,
                    R.color.login_notification_accent_color);
        }
    }

    static class OnCredentialsOK {
        OnCredentialsOK() {}
    }

    @Inject Dispatcher mDispatcher;

    @Inject LoginAnalyticsListener mAnalyticsListener;

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
        clearServiceState(LoginState.class);
    }

    public LoginWpcomService() {
        super(new LoginState(LoginStep.IDLE));
    }

    @Override
    protected void onProgressStart() {
        mDispatcher.register(this);
    }

    @Override
    protected void onProgressEnd() {
        mDispatcher.unregister(this);
    }

    @Override
    public Notification getNotification(LoginState state) {
        switch (state.getStep()) {
            case AUTHENTICATING:
            case SOCIAL_LOGIN:
            case FETCHING_ACCOUNT:
            case FETCHING_SETTINGS:
            case FETCHING_SITES:
                return LoginNotification.progress(this, state.getStep().progressPercent);
            case SUCCESS:
                return LoginNotification.success(this);
            case FAILURE_EMAIL_WRONG_PASSWORD:
                return LoginNotification.failure(this, R.string.notification_error_wrong_password);
            case FAILURE_2FA:
                return LoginNotification.failure(this, R.string.notification_2fa_needed);
            case FAILURE_SOCIAL_2FA:
                return LoginNotification.failure(this, R.string.notification_2fa_needed);
            case FAILURE_USE_WPCOM_USERNAME_INSTEAD_OF_EMAIL:
                return LoginNotification.failure(this, R.string.notification_wpcom_username_needed);
            case FAILURE_FETCHING_ACCOUNT:
            case FAILURE_CANNOT_ADD_DUPLICATE_SITE:
            case FAILURE:
                return LoginNotification.failure(this, R.string.notification_login_failed);
        }

        return null;
    }

    @Override
    protected void trackStateUpdate(Map<String, ?> props) {
        mAnalyticsListener.trackWpComBackgroundServiceUpdate(props);
    }

    private void setState(LoginStep phase) {
        setState(new LoginState(phase));
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

        setState(LoginStep.AUTHENTICATING);

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
            mAnalyticsListener.trackLoginFailed(error.getClass().getSimpleName(),
                    error.toString(), errorMessage);

            if (mIsSocialLogin) {
                mAnalyticsListener.trackSocialFailure(error.getClass().getSimpleName(),
                        error.toString(), errorMessage);
            }
        }

        switch (error) {
            case INCORRECT_USERNAME_OR_PASSWORD:
            case NOT_AUTHENTICATED: // NOT_AUTHENTICATED is the generic error from XMLRPC response on first call.
                setState(LoginStep.FAILURE_EMAIL_WRONG_PASSWORD);
                break;
            case NEEDS_2FA:
                // login credentials were correct anyway so, offer to save to SmartLock
                signalCredentialsOK();

                if (mIsSocialLogin) {
                    setState(LoginStep.FAILURE_SOCIAL_2FA);
                } else {
                    setState(LoginStep.FAILURE_2FA);
                }

                break;
            case EMAIL_LOGIN_NOT_ALLOWED:
                setState(LoginStep.FAILURE_USE_WPCOM_USERNAME_INSTEAD_OF_EMAIL);
                break;
            case INVALID_REQUEST:
                // TODO: FluxC: could be specific?
            default:
                setState(LoginStep.FAILURE);
                AppLog.e(T.NUX, "Server response: " + errorMessage);

                ToastUtils.showToast(this, errorMessage == null ? getString(R.string.error_generic) : errorMessage);
                break;
        }
    }

    private void fetchAccount() {
        setState(LoginStep.FETCHING_ACCOUNT);
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
            setState(LoginStep.SOCIAL_LOGIN);
            PushSocialPayload payload = new PushSocialPayload(mIdToken, mService);
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
            mAnalyticsListener.trackSocialConnectFailure();
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
            mAnalyticsListener.trackSocialConnectSuccess();
            fetchAccount();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (event.isError()) {
            AppLog.e(T.API, "onAccountChanged has error: " + event.error.type + " - " + event.error.message);
            setState(LoginStep.FAILURE_FETCHING_ACCOUNT);
            return;
        }

        if (event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
            setState(LoginStep.FETCHING_SETTINGS);
            // The user's account info has been fetched and stored - next, fetch the user's settings
            mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
            setState(LoginStep.FETCHING_SITES);
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
                setState(LoginStep.FAILURE);
                return;
            }

            if (event.rowsAffected == 0) {
                // If there is a duplicate site and not any site has been added, show an error and
                // stop the sign in process
                setState(LoginStep.FAILURE_CANNOT_ADD_DUPLICATE_SITE);
                return;
            } else {
                // If there is a duplicate site, notify the user something could be wrong,
                // but continue the sign in process
                ToastUtils.showToast(this, R.string.duplicate_site_detected);
            }
        }

        setState(LoginStep.SUCCESS);
    }
}
