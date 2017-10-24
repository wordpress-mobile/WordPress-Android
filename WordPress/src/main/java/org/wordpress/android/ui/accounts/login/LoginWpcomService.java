package org.wordpress.android.ui.accounts.login;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.accounts.login.LoginWpcomService.OnLoginStateUpdated;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateService;
import org.wordpress.android.ui.reader.services.ReaderUpdateService;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutoForeground;
import org.wordpress.android.util.SiteUtils;

import java.util.ArrayList;
import java.util.EnumSet;

import javax.inject.Inject;

public class LoginWpcomService extends AutoForeground<OnLoginStateUpdated> {

    private static final String ARG_EMAIL = "ARG_EMAIL";
    private static final String ARG_PASSWORD = "ARG_PASSWORD";
    private static final String ARG_SOCIAL_ID_TOKEN = "ARG_SOCIAL_ID_TOKEN";
    private static final String ARG_SOCIAL_LOGIN = "ARG_SOCIAL_LOGIN";
    private static final String ARG_SOCIAL_SERVICE = "ARG_SOCIAL_SERVICE";

    public enum LoginPhase {
        IDLE,
        AUTHENTICATING,
        FETCHING_ACCOUNT,
        FETCHING_SETTINGS,
        FETCHING_SITES,
        SUCCESS,
        FAILURE
    }

    public static class OnLoginStateUpdated {
        public final LoginPhase state;

        public OnLoginStateUpdated(LoginPhase state) {
            this.state = state;
        }
    }

    public static class OnCredentialsOK {
        public OnCredentialsOK() {}
    }

    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    private LoginPhase mLoginPhase = LoginPhase.IDLE;

    private String mIdToken;
    private String mService;
    private boolean isSocialLogin;

    private ArrayList<Integer> mOldSitesIDs;

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

    public LoginWpcomService() {
        super(OnLoginStateUpdated.class);
    }

    @Override
    protected OnLoginStateUpdated getCurrentStateEvent() {
        return new OnLoginStateUpdated(mLoginPhase);
    }

    @Override
    public boolean isInProgress() {
        return mLoginPhase != LoginPhase.IDLE
                && mLoginPhase != LoginPhase.SUCCESS
                && mLoginPhase != LoginPhase.FAILURE;
    }

    @Override
    public boolean isError() {
        return mLoginPhase == LoginPhase.FAILURE;
    }

    @Override
    public Notification getNotification() {
        switch (mLoginPhase) {
            case AUTHENTICATING:
                return getProgressNotification(25, "Login in: " + mLoginPhase.name());
            case FETCHING_ACCOUNT:
                return getProgressNotification(50, "Login in: " + mLoginPhase.name());
            case FETCHING_SETTINGS:
                return getProgressNotification(75, "Login in: " + mLoginPhase.name());
            case FETCHING_SITES:
                return getProgressNotification(100, "Login in: " + mLoginPhase.name());
            case SUCCESS:
                return getSuccessNotification("Logged in!");
            case FAILURE:
                return getFailureNotification("Login failed :(");
        }

        return null;
    }

    private void setState(LoginPhase loginPhase) {
        mLoginPhase = loginPhase;
        notifyState();

        if (loginPhase == LoginPhase.FAILURE || loginPhase == LoginPhase.SUCCESS) {
            stopSelf();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);

        AppLog.i(T.MAIN, "LoginWpcomService > Created");
        mDispatcher.register(this);

        // TODO: Recover any login attempts that were interrupted by the service being stopped?
    }

    @Override
    public void onDestroy() {
        mDispatcher.unregister(this);
        AppLog.i(T.MAIN, "LoginWpcomService > Destroyed");
        super.onDestroy();
    }

    private Intent getPendingIntent() {
        return new Intent(this, WPMainActivity.class);
    }

    private Notification getProgressNotification(int progress, String content) {
        return new NotificationCompat.Builder(this)
                .setContentTitle(content)
                .setSmallIcon(R.drawable.ic_my_sites_24dp)
                .setColor(getResources().getColor(R.color.blue_wordpress))
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.mipmap.app_icon))
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(LoginWpcomService.this,
                        AutoForeground.NOTIFICATION_ID_PROGRESS,
                        getPendingIntent(),
                        PendingIntent.FLAG_ONE_SHOT))
                .setProgress(100, progress, false)
                .build();
    }

    private Notification getSuccessNotification(String content) {
        return new NotificationCompat.Builder(this)
                .setContentTitle(content)
                .setSmallIcon(R.drawable.ic_my_sites_24dp)
                .setColor(getResources().getColor(R.color.blue_wordpress))
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.mipmap.app_icon))
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(LoginWpcomService.this,
                        AutoForeground.NOTIFICATION_ID_SUCCESS,
                        getPendingIntent(),
                        PendingIntent.FLAG_ONE_SHOT))
                .build();
    }

    private Notification getFailureNotification(String content) {
        return new NotificationCompat.Builder(this)
                .setContentTitle(content)
                .setSmallIcon(R.drawable.ic_my_sites_24dp)
                .setColor(getResources().getColor(R.color.blue_wordpress))
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.mipmap.app_icon))
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(LoginWpcomService.this,
                        AutoForeground.NOTIFICATION_ID_FAILURE,
                        getPendingIntent(),
                        PendingIntent.FLAG_ONE_SHOT))
                .build();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        setState(LoginPhase.AUTHENTICATING);

        final String email = intent.getStringExtra(ARG_EMAIL);
        final String password = intent.getStringExtra(ARG_PASSWORD);

        mIdToken = intent.getStringExtra(ARG_SOCIAL_ID_TOKEN);
        mService = intent.getStringExtra(ARG_SOCIAL_SERVICE);
        isSocialLogin = intent.getBooleanExtra(ARG_SOCIAL_LOGIN, false);

        mOldSitesIDs = SiteUtils.getCurrentSiteIds(mSiteStore, false);

        AccountStore.AuthenticatePayload payload = new AccountStore.AuthenticatePayload(email, password);
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        AppLog.i(T.NUX, "User tries to log in wpcom. Email: " + email);

        return START_REDELIVER_INTENT;
    }

//    private void handleAuthError(AccountStore.AuthenticationErrorType error, String errorMessage) {
//        if (error != AccountStore.AuthenticationErrorType.NEEDS_2FA) {
//            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FAILED, error.getClass().getSimpleName(),
//                    error.toString(), errorMessage);
//
//            if (isSocialLogin) {
//                AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_FAILURE, error.getClass().getSimpleName(),
//                        error.toString(), errorMessage);
//            }
//        }
//
//        switch (error) {
//            case INCORRECT_USERNAME_OR_PASSWORD:
//            case NOT_AUTHENTICATED: // NOT_AUTHENTICATED is the generic error from XMLRPC response on first call.
//                showPasswordError();
//                break;
//            case NEEDS_2FA:
//                // login credentials were correct anyway so, offer to save to SmartLock
//                saveCredentialsInSmartLock(mLoginListener.getSmartLockHelper(), mEmailAddress, mPassword);
//
//                if (isSocialLogin) {
//                    mLoginListener.needs2faSocialConnect(mEmailAddress, mRequestedPassword, mIdToken, mService);
//                } else {
//                    mLoginListener.needs2fa(mEmailAddress, mRequestedPassword);
//                }
//
//                break;
//            case INVALID_REQUEST:
//                // TODO: FluxC: could be specific?
//            default:
//                AppLog.e(T.NUX, "Server response: " + errorMessage);
//
//                ToastUtils.showToast(getActivity(),
//                        errorMessage == null ? getString(R.string.error_generic) : errorMessage);
//                break;
//        }
//    }

    protected void startPostLoginServices() {
        // Get reader tags so they're available as soon as the Reader is accessed - done for
        // both wp.com and self-hosted (self-hosted = "logged out" reader) - note that this
        // uses the application context since the activity is finished immediately below
        ReaderUpdateService.startService(getApplicationContext(), EnumSet.of(ReaderUpdateService
                .UpdateTask.TAGS));

        // Start Notification service
        NotificationsUpdateService.startService(getApplicationContext());
    }

    private void doFinishLogin() {
        startPostLoginServices();
        setState(LoginPhase.FETCHING_ACCOUNT);
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
    }

    private EventBus getEventBus() {
        return EventBus.getDefault();
    }

    private void signalCredentialsOK() {
        getEventBus().post(new OnCredentialsOK());
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            AppLog.e(T.API, "onAuthenticationChanged has error: " + event.error.type + " - " + event.error.message);
            setState(LoginPhase.FAILURE);
//            handleAuthError(event.error.type, event.error.message);
            return;
        }

        AppLog.i(T.NUX, "onAuthenticationChanged: " + event.toString());

        if (isSocialLogin) {
            AccountStore.PushSocialLoginPayload payload = new AccountStore.PushSocialLoginPayload(mIdToken, mService);
            mDispatcher.dispatch(AccountActionBuilder.newPushSocialConnectAction(payload));
        } else {
            signalCredentialsOK();
            doFinishLogin();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocialChanged(AccountStore.OnSocialChanged event) {
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

            doFinishLogin();
        } else if (!event.requiresTwoStepAuth) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_CONNECT_SUCCESS);
            doFinishLogin();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(AccountStore.OnAccountChanged event) {
        if (event.isError()) {
            AppLog.e(AppLog.T.API, "onAccountChanged has error: " + event.error.type + " - " + event.error.message);
//            ToastUtils.showToast(getContext(), R.string.error_fetch_my_profile);
//            onLoginFinished(false);
            setState(LoginPhase.FAILURE);
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
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
        if (event.isError()) {
            AppLog.e(AppLog.T.API, "onSiteChanged has error: " + event.error.type + " - " + event.error.toString());
            if (event.error.type != SiteStore.SiteErrorType.DUPLICATE_SITE) {
//                onLoginFinished(false);
                setState(LoginPhase.FAILURE);
                return;
            }

            if (event.rowsAffected == 0) {
                // If there is a duplicate site and not any site has been added, show an error and
                // stop the sign in process
//                ToastUtils.showToast(getContext(), R.string.cannot_add_duplicate_site);
//                onLoginFinished(false);
                setState(LoginPhase.FAILURE);
                return;
            } else {
                // If there is a duplicate site, notify the user something could be wrong,
                // but continue the sign in process
//                ToastUtils.showToast(getContext(), R.string.duplicate_site_detected);
            }
        }

        startPostLoginServices();

        AnalyticsUtils.trackAnalyticsSignIn(mAccountStore, mSiteStore, true);

        setState(LoginPhase.SUCCESS);
    }
}
