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

import dagger.android.support.AndroidSupportInjection;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class LoginGoogleFragment extends GoogleFragment {
    private static final int REQUEST_LOGIN = 1001;

    public static final String TAG = "login_google_fragment_tag";

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);

        switch (request) {
            case REQUEST_LOGIN:
                if (result == RESULT_OK) {
                    GoogleSignInResult signInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

                    if (signInResult.isSuccess()) {
                        try {
                            GoogleSignInAccount account = signInResult.getSignInAccount();

                            if (account != null) {
                                mGoogleEmail = account.getEmail() != null ? account.getEmail() : "";
                                mGoogleListener.onGoogleEmailSelected(mGoogleEmail);
                                mIdToken = account.getIdToken() != null ? account.getIdToken() : "";
                            }

                            PushSocialPayload payload = new PushSocialPayload(mIdToken, SERVICE_TYPE_GOOGLE);
                            mDispatcher.dispatch(AccountActionBuilder.newPushSocialLoginAction(payload));
                        } catch (NullPointerException exception) {
                            disconnectGoogleClient();
                            AppLog.e(T.NUX, "Cannot get ID token from Google login account.", exception);
                            showErrorDialog(getString(R.string.login_error_generic));
                        }
                    } else {
                        mAnalyticsListener.trackSocialButtonFailure();
                        switch (signInResult.getStatus().getStatusCode()) {
                            // Internal error.
                            case GoogleSignInStatusCodes.INTERNAL_ERROR:
                                AppLog.e(T.NUX, "Google Login Failed: internal error.");
                                showErrorDialog(getString(R.string.login_error_generic));
                                break;
                            // Attempted to connect with an invalid account name specified.
                            case GoogleSignInStatusCodes.INVALID_ACCOUNT:
                                AppLog.e(T.NUX, "Google Login Failed: invalid account name.");
                                showErrorDialog(getString(R.string.login_error_generic)
                                        + getString(R.string.login_error_suffix));
                                break;
                            // Network error.
                            case GoogleSignInStatusCodes.NETWORK_ERROR:
                                AppLog.e(T.NUX, "Google Login Failed: network error.");
                                showErrorDialog(getString(R.string.error_generic_network));
                                break;
                            // Cancelled by the user.
                            case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                                AppLog.e(T.NUX, "Google Login Failed: cancelled by user.");
                                break;
                            // Attempt didn't succeed with the current account.
                            case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                                AppLog.e(T.NUX, "Google Login Failed: current account failed.");
                                showErrorDialog(getString(R.string.login_error_generic));
                                break;
                            // Attempted to connect, but the user is not signed in.
                            case GoogleSignInStatusCodes.SIGN_IN_REQUIRED:
                                AppLog.e(T.NUX, "Google Login Failed: user is not signed in.");
                                showErrorDialog(getString(R.string.login_error_generic));
                                break;
                            // Timeout error.
                            case GoogleSignInStatusCodes.TIMEOUT:
                                AppLog.e(T.NUX, "Google Login Failed: timeout error.");
                                showErrorDialog(getString(R.string.google_error_timeout));
                                break;
                            // Unknown error.
                            default:
                                AppLog.e(T.NUX, "Google Login Failed: unknown error.");
                                showErrorDialog(getString(R.string.login_error_generic));
                                break;
                        }
                    }
                } else if (result == RESULT_CANCELED) {
                    mAnalyticsListener.trackSocialButtonFailure();
                    AppLog.e(T.NUX, "Google Login Failed: result was CANCELED.");
                } else {
                    mAnalyticsListener.trackSocialButtonFailure();
                    AppLog.e(T.NUX, "Google Login Failed: result was not OK or CANCELED.");
                    showErrorDialog(getString(R.string.login_error_generic));
                }

                break;
        }
    }

    @Override
    protected void showAccountDialog() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, REQUEST_LOGIN);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        disconnectGoogleClient();

        if (event.isError()) {
            AppLog.e(T.API, "LoginGoogleFragment.onAuthenticationChanged: " + event.error.type
                    + " - " + event.error.message);
            mAnalyticsListener.trackLoginFailed(event.getClass().getSimpleName(),
                    event.error.type.toString(), event.error.message);

            mAnalyticsListener.trackSocialFailure(event.getClass().getSimpleName(),
                    event.error.type.toString(), event.error.message);

            showErrorDialog(getString(R.string.login_error_generic));
        } else {
            AppLog.i(T.NUX, "LoginGoogleFragment.onAuthenticationChanged: " + event.toString());
            mGoogleListener.onGoogleLoginFinished();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocialChanged(OnSocialChanged event) {
        disconnectGoogleClient();

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
                    mAnalyticsListener.trackSocialAccountsNeedConnecting();
                    mLoginListener.loginViaSocialAccount(mGoogleEmail, mIdToken, SERVICE_TYPE_GOOGLE, true);
                    break;
                // WordPress account does not exist with input email address.
                case UNKNOWN_USER:
                    mAnalyticsListener.trackSocialErrorUnknownUser();
                    showErrorDialog(getString(R.string.login_error_email_not_found, mGoogleEmail));
                    break;
                // Unknown error.
                case GENERIC_ERROR:
                // Do nothing for now (included to show all error types) and just fall through to 'default'
                default:
                    showErrorDialog(getString(R.string.login_error_generic));
                    break;
            }
        // Response does not return error when two-factor authentication is required.
        } else if (event.requiresTwoStepAuth) {
            mLoginListener.needs2faSocial(mGoogleEmail, event.userId, event.nonceAuthenticator, event.nonceBackup,
                    event.nonceSms);
        } else {
            mGoogleListener.onGoogleLoginFinished();
        }
    }
}
