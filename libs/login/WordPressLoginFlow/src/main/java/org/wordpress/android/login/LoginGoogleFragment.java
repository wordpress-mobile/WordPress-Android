package org.wordpress.android.login;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ContextThemeWrapper;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore.AccountSocialErrorType;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnSocialChanged;
import org.wordpress.android.fluxc.store.AccountStore.PushSocialPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class LoginGoogleFragment extends Fragment implements ConnectionCallbacks, OnConnectionFailedListener {
    private GoogleApiClient mGoogleApiClient;
    private GoogleLoginListener mGoogleLoginListener;
    private LoginListener mLoginListener;
    private String mGoogleEmail;
    private String mIdToken;
    private boolean mIsResolvingError;
    private boolean mShouldResolveError;

    private static final String SERVICE_TYPE_GOOGLE = "google";
    private static final String STATE_RESOLVING_ERROR = "STATE_RESOLVING_ERROR";
    private static final int REQUEST_CONNECT = 1000;
    private static final int REQUEST_LOGIN = 1001;

    public static final String TAG = "login_google_fragment_tag";

    @Inject Dispatcher mDispatcher;

    @Inject LoginAnalyticsListener mAnalyticsListener;

    public interface GoogleLoginListener {
        void onGoogleEmailSelected(String email);
        void onGoogleLoginFinished();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore state of error resolving.
        mIsResolvingError = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        // Configure sign-in to request user's ID, basic profile, email address, and ID token.
        // ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(getString(R.string.default_web_client_id))
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestProfile()
                .requestEmail()
                .build();

        // Build Google API client with access to sign-in API and options specified above.
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (!mIsResolvingError) {
            connectGoogleClient();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mIsResolvingError);
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
        mLoginListener = (LoginListener) context;

        try {
            mGoogleLoginListener = (GoogleLoginListener) context;
        } catch (ClassCastException exception) {
            throw new ClassCastException(context.toString() + " must implement GoogleLoginListener");
        }

        // Show account dialog when Google API onConnected callback returns before fragment is attached.
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected() && !mIsResolvingError && !mShouldResolveError) {
            showAccountDialog();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginListener = null;
        disconnectGoogleClient();
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mDispatcher.unregister(this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        // Indicates account was selected, that account has granted any required permissions, and a
        // connection to Google Play services has been established.
        if (mShouldResolveError) {
            mShouldResolveError = false;

            if (isAdded()) {
                showAccountDialog();
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Could not connect to Google Play Services.  The user needs to select an account, grant
        // permissions or resolve an error in order to sign in.  Refer to the documentation for
        // ConnectionResult to see possible error codes.
        if (!mIsResolvingError && mShouldResolveError) {
            if (connectionResult.hasResolution()) {
                try {
                    mIsResolvingError = true;
                    connectionResult.startResolutionForResult(getActivity(), REQUEST_CONNECT);
                } catch (IntentSender.SendIntentException exception) {
                    mIsResolvingError = false;
                    mGoogleApiClient.connect();
                }
            } else {
                mIsResolvingError = false;
                AppLog.e(T.NUX, GoogleApiAvailability.getInstance().getErrorString(connectionResult.getErrorCode()));
                showErrorDialog(getString(R.string.login_error_generic));
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Connection to Google Play services was lost.  GoogleApiClient will automatically attempt
        // to re-connect.  Any UI elements depending on connection to Google APIs should be hidden
        // or disabled until onConnected is called again.
        Log.w(LoginGoogleFragment.class.getSimpleName(), "onConnectionSuspended: " + i);
    }

    public void connectGoogleClient() {
        if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
            mShouldResolveError = true;
            mGoogleApiClient.connect();
        } else {
            showAccountDialog();
        }
    }

    private void disconnectGoogleClient() {
        if (mGoogleApiClient.isConnected()) {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }
    }

    private void showAccountDialog() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, REQUEST_LOGIN);
    }

    private void showErrorDialog(String message) {
        AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.LoginTheme))
                .setMessage(message)
                .setPositiveButton(R.string.login_error_button, null)
                .create();
        dialog.show();
    }

    @Override
    public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);

        switch (request) {
            case REQUEST_CONNECT:
                if (result != RESULT_OK) {
                    mShouldResolveError = false;
                }

                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                } else {
                    showAccountDialog();
                }

                mIsResolvingError = false;
                break;
            case REQUEST_LOGIN:
                if (result == RESULT_OK) {
                    GoogleSignInResult signInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

                    if (signInResult.isSuccess()) {
                        try {
                            GoogleSignInAccount account = signInResult.getSignInAccount();
                            mGoogleEmail = account.getEmail();
                            mGoogleLoginListener.onGoogleEmailSelected(mGoogleEmail);
                            mIdToken = account.getIdToken();
                            PushSocialPayload payload = new PushSocialPayload(mIdToken, SERVICE_TYPE_GOOGLE);
                            mDispatcher.dispatch(AccountActionBuilder.newPushSocialLoginAction(payload));
                        } catch (NullPointerException exception) {
                            disconnectGoogleClient();
                            AppLog.e(T.NUX, "Cannot get ID token from Google sign-in account.", exception);
                            showErrorDialog(getString(R.string.login_error_generic));
                        }
                    } else {
                        mAnalyticsListener.trackSocialButtonFailure();
                        switch (signInResult.getStatus().getStatusCode()) {
                            // Internal error.
                            case GoogleSignInStatusCodes.INTERNAL_ERROR:
                                AppLog.e(T.NUX, "Google Sign-in Failed: internal error.");
                                showErrorDialog(getString(R.string.login_error_generic));
                                break;
                            // Attempted to connect with an invalid account name specified.
                            case GoogleSignInStatusCodes.INVALID_ACCOUNT:
                                AppLog.e(T.NUX, "Google Sign-in Failed: invalid account name.");
                                showErrorDialog(getString(R.string.login_error_generic)
                                        + getString(R.string.login_error_suffix));
                                break;
                            // Network error.
                            case GoogleSignInStatusCodes.NETWORK_ERROR:
                                AppLog.e(T.NUX, "Google Sign-in Failed: network error.");
                                showErrorDialog(getString(R.string.error_generic_network));
                                break;
                            // Cancelled by the user.
                            case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                                AppLog.e(T.NUX, "Google Sign-in Failed: cancelled by user.");
                                break;
                            // Attempt didn't succeed with the current account.
                            case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                                AppLog.e(T.NUX, "Google Sign-in Failed: current account failed.");
                                showErrorDialog(getString(R.string.login_error_generic));
                                break;
                            // Attempted to connect, but the user is not signed in.
                            case GoogleSignInStatusCodes.SIGN_IN_REQUIRED:
                                AppLog.e(T.NUX, "Google Sign-in Failed: user is not signed in.");
                                showErrorDialog(getString(R.string.login_error_generic));
                                break;
                            // Unknown error.
                            default:
                                AppLog.e(T.NUX, "Google Sign-in Failed: unknown error.");
                                showErrorDialog(getString(R.string.login_error_generic));
                                break;
                        }
                    }
                } else if (result == RESULT_CANCELED) {
                    mAnalyticsListener.trackSocialButtonFailure();
                    AppLog.e(T.NUX, "Google Sign-in Failed: result was CANCELED.");
                } else {
                    mAnalyticsListener.trackSocialButtonFailure();
                    AppLog.e(T.NUX, "Google Sign-in Failed: result was not OK or CANCELED.");
                    showErrorDialog(getString(R.string.login_error_generic));
                }

                break;
        }
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
            mGoogleLoginListener.onGoogleLoginFinished();
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
            mGoogleLoginListener.onGoogleLoginFinished();
        }
    }
}
