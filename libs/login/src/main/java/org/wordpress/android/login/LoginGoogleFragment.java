package org.wordpress.android.login;

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore.AccountSocialErrorType;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnSocialChanged;
import org.wordpress.android.fluxc.store.AccountStore.PushSocialPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import dagger.android.support.AndroidSupportInjection;

public class LoginGoogleFragment extends GoogleFragment {
    private static final int REQUEST_LOGIN = 1001;
    private boolean mLoginRequested = false;
    private boolean mIsSignupFromLoginEnabled = true;

    public static final String TAG = "login_google_fragment_tag";

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    protected void startFlow() {
        if (!mLoginRequested) {
            AppLog.d(T.MAIN, "GOOGLE LOGIN: startFlow");
            mLoginRequested = true;
            Intent loginIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            startActivityForResult(loginIntent, REQUEST_LOGIN);
        } else {
            AppLog.d(T.MAIN, "GOOGLE LOGIN: startFlow called, but is already in progress");
        }
    }

    @Override
    public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);

        switch (request) {
            case REQUEST_LOGIN:
                disconnectGoogleClient();
                mLoginRequested = false;
                if (result == RESULT_OK) {
                    AppLog.d(T.MAIN, "GOOGLE LOGIN: Google has returned a sign in result - succcess");
                    GoogleSignInResult loginResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

                    if (loginResult.isSuccess()) {
                        try {
                            GoogleSignInAccount account = loginResult.getSignInAccount();

                            if (account != null) {
                                mDisplayName = account.getDisplayName() != null ? account.getDisplayName() : "";
                                mGoogleEmail = account.getEmail() != null ? account.getEmail() : "";
                                mGoogleListener.onGoogleEmailSelected(mGoogleEmail);
                                mIdToken = account.getIdToken() != null ? account.getIdToken() : "";
                                mPhotoUrl = removeScaleFromGooglePhotoUrl(
                                        account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "");
                            }

                            AppLog.d(T.MAIN,
                                    "GOOGLE LOGIN: Google has returned a sign in result - dispatching "
                                    + "SocialLoginAction");
                            PushSocialPayload payload = new PushSocialPayload(mIdToken, SERVICE_TYPE_GOOGLE);
                            mDispatcher.dispatch(AccountActionBuilder.newPushSocialLoginAction(payload));
                        } catch (NullPointerException exception) {
                            AppLog.d(T.MAIN, "GOOGLE LOGIN: Google has returned a sign in result - NPE");
                            AppLog.e(T.NUX, "Cannot get ID token from Google login account.", exception);
                            showError(getString(R.string.login_error_generic));
                        }
                    } else {
                        AppLog.d(T.MAIN, "GOOGLE LOGIN: Google has returned a sign in result - error");
                        mAnalyticsListener.trackSocialButtonFailure();
                        switch (loginResult.getStatus().getStatusCode()) {
                            // Internal error.
                            case GoogleSignInStatusCodes.INTERNAL_ERROR:
                                AppLog.e(T.NUX, "Google Login Failed: internal error.");
                                showError(getString(R.string.login_error_generic));
                                break;
                            // Attempted to connect with an invalid account name specified.
                            case GoogleSignInStatusCodes.INVALID_ACCOUNT:
                                AppLog.e(T.NUX, "Google Login Failed: invalid account name.");
                                showError(getString(R.string.login_error_generic)
                                          + getString(R.string.login_error_suffix));
                                break;
                            // Network error.
                            case GoogleSignInStatusCodes.NETWORK_ERROR:
                                AppLog.e(T.NUX, "Google Login Failed: network error.");
                                showError(getString(R.string.error_generic_network));
                                break;
                            // Cancelled by the user.
                            case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                                AppLog.e(T.NUX, "Google Login Failed: cancelled by user.");
                                break;
                            // Attempt didn't succeed with the current account.
                            case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                                AppLog.e(T.NUX, "Google Login Failed: current account failed.");
                                showError(getString(R.string.login_error_generic));
                                break;
                            // Attempted to connect, but the user is not signed in.
                            case GoogleSignInStatusCodes.SIGN_IN_REQUIRED:
                                AppLog.e(T.NUX, "Google Login Failed: user is not signed in.");
                                showError(getString(R.string.login_error_generic));
                                break;
                            // Timeout error.
                            case GoogleSignInStatusCodes.TIMEOUT:
                                AppLog.e(T.NUX, "Google Login Failed: timeout error.");
                                showError(getString(R.string.google_error_timeout));
                                break;
                            // Unknown error.
                            default:
                                AppLog.e(T.NUX, "Google Login Failed: unknown error.");
                                showError(getString(R.string.login_error_generic));
                                break;
                        }
                    }
                } else if (result == RESULT_CANCELED) {
                    AppLog.d(T.MAIN, "GOOGLE LOGIN: Google has returned a sign in result - canceled");
                    mAnalyticsListener.trackSocialButtonFailure();
                    AppLog.e(T.NUX, "Google Login Failed: result was CANCELED.");
                    finishFlow();
                } else {
                    AppLog.d(T.MAIN, "GOOGLE LOGIN: Google has returned a sign in result - unknown");
                    mAnalyticsListener.trackSocialButtonFailure();
                    AppLog.e(T.NUX, "Google Login Failed: result was not OK or CANCELED.");
                    showError(getString(R.string.login_error_generic));
                }

                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            AppLog.d(T.MAIN, "GOOGLE LOGIN: onAuthenticationChanged - error");
            AppLog.e(T.API, "LoginGoogleFragment.onAuthenticationChanged: " + event.error.type
                            + " - " + event.error.message);
            mAnalyticsListener.trackLoginFailed(event.getClass().getSimpleName(),
                    event.error.type.toString(), event.error.message);

            mAnalyticsListener.trackSocialFailure(event.getClass().getSimpleName(),
                    event.error.type.toString(), event.error.message);

            showError(getString(R.string.login_error_generic));
        } else if (event.createdAccount) {
            AppLog.d(T.MAIN,
                    "GOOGLE SIGNUP: onAuthenticationChanged - new wordpress account created");
            mAnalyticsListener.trackCreatedAccount(event.userName, mGoogleEmail);
            mAnalyticsListener.trackAnalyticsSignIn(true);
            mGoogleListener.onGoogleSignupFinished(mDisplayName, mGoogleEmail, mPhotoUrl, event.userName);
        } else {
            AppLog.d(T.MAIN, "GOOGLE LOGIN: onAuthenticationChanged - success");
            AppLog.i(T.NUX, "LoginGoogleFragment.onAuthenticationChanged: " + event.toString());
            mGoogleListener.onGoogleLoginFinished();
            finishFlow();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocialChanged(OnSocialChanged event) {
        // Response returns error for non-existing account and existing account not connected.
        if (event.isError()) {
            AppLog.e(T.API, "LoginGoogleFragment.onSocialChanged: " + event.error.type + " - " + event.error.message);

            if (event.error.type != AccountSocialErrorType.USER_EXISTS) {
                mAnalyticsListener.trackLoginFailed(event.getClass().getSimpleName(),
                        event.error.type.toString(), event.error.message);

                mAnalyticsListener.trackSocialFailure(event.getClass().getSimpleName(),
                        event.error.type.toString(), event.error.message);
            }

            switch (event.error.type) {
                // WordPress account exists with input email address, but not connected.
                case USER_EXISTS:
                    AppLog.d(T.MAIN, "GOOGLE LOGIN: onSocialChanged - wordpress acount exists but not connected");
                    mAnalyticsListener.trackSocialAccountsNeedConnecting();
                    mLoginListener.loginViaSocialAccount(mGoogleEmail, mIdToken, SERVICE_TYPE_GOOGLE, true);
                    finishFlow();
                    break;
                // WordPress account does not exist with input email address.
                case UNKNOWN_USER:
                    if (mIsSignupFromLoginEnabled) {
                        PushSocialPayload payload = new PushSocialPayload(mIdToken, SERVICE_TYPE_GOOGLE);
                        AppLog.d(T.MAIN, "GOOGLE SIGNUP: sign up result returned - dispatching SocialSignupAction");
                        mDispatcher.dispatch(AccountActionBuilder.newPushSocialSignupAction(payload));
                    } else {
                        AppLog.d(T.MAIN, "GOOGLE LOGIN: onSocialChanged - wordpress acount doesn't exist");
                        mAnalyticsListener.trackSocialErrorUnknownUser();
                        showError(getString(R.string.login_error_email_not_found_v2));
                    }
                    break;
                // Too many attempts on sending SMS verification code. The user has to wait before they try again
                case SMS_CODE_THROTTLED:
                    AppLog.d(T.MAIN, "GOOGLE LOGIN: onSocialChanged - error - sms code throttled");
                    showError(getString(R.string.login_error_sms_throttled));
                    break;
                // Unknown error.
                case GENERIC_ERROR:
                // Do nothing for now (included to show all error types) and just fall through to 'default'
                default:
                    AppLog.d(T.MAIN, "GOOGLE LOGIN: onSocialChanged - unknown error");
                    showError(getString(R.string.login_error_generic));
                    break;
            }
        // Response does not return error when two-factor authentication is required.
        } else if (event.requiresTwoStepAuth || Login2FaFragment.TWO_FACTOR_TYPE_SMS.equals(event.notificationSent)) {
            AppLog.d(T.MAIN, "GOOGLE LOGIN: onSocialChanged - needs 2fa");
            mLoginListener.needs2faSocial(mGoogleEmail, event.userId, event.nonceAuthenticator, event.nonceBackup,
                    event.nonceSms);
            finishFlow();
        } else {
            AppLog.d(T.MAIN, "GOOGLE LOGIN: onSocialChanged - success");
            mGoogleListener.onGoogleLoginFinished();
            finishFlow();
        }
    }
}
