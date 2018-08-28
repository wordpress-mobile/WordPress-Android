package org.wordpress.android.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnSocialChanged;
import org.wordpress.android.fluxc.store.AccountStore.PushSocialPayload;
import org.wordpress.android.login.util.SiteUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dagger.android.support.AndroidSupportInjection;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class SignupGoogleFragment extends GoogleFragment {
    private static final String OLD_SITES_IDS = "old_sites_ids";
    private static final String SIGN_UP_REQUESTED = "sign_up_requested";
    private ArrayList<Integer> mOldSitesIds;
    private ProgressDialog mProgressDialog;
    private boolean mSignupRequested;

    private static final int REQUEST_SIGNUP = 1002;

    public static final String TAG = "signup_google_fragment_tag";

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        mProgressDialog = ProgressDialog.show(
                getActivity(), null, getString(R.string.signup_with_google_progress), true, false, null);
        super.onAttach(context);
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mOldSitesIds = savedInstanceState.getIntegerArrayList(OLD_SITES_IDS);
            mSignupRequested = savedInstanceState.getBoolean(SIGN_UP_REQUESTED);
        }
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList(OLD_SITES_IDS, mOldSitesIds);
        outState.putBoolean(SIGN_UP_REQUESTED, mSignupRequested);
    }

    @Override
    public void onDetach() {
        dismissProgressDialog();
        super.onDetach();
    }

    @Override
    protected void startSignInProcess() {
        if (!mSignupRequested) {
            AppLog.d(T.MAIN, "GOOGLE SIGNUP: startSignInProcess");
            mSignupRequested = true;
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            startActivityForResult(signInIntent, REQUEST_SIGNUP);
        } else {
            AppLog.d(T.MAIN, "GOOGLE SIGNUP: startSignInProcess called, but is already in progress");
        }
    }

    @Override
    public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);

        switch (request) {
            case REQUEST_SIGNUP:
                disconnectGoogleClient();
                mSignupRequested = false;
                if (result == RESULT_OK) {
                    GoogleSignInResult signInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

                    if (signInResult.isSuccess()) {
                        AppLog.d(T.MAIN, "GOOGLE SIGNUP: Google has returned a sign up result - succcess");
                        try {
                            GoogleSignInAccount account = signInResult.getSignInAccount();

                            if (account != null) {
                                mDisplayName = account.getDisplayName() != null ? account.getDisplayName() : "";
                                mGoogleEmail = account.getEmail() != null ? account.getEmail() : "";
                                mIdToken = account.getIdToken() != null ? account.getIdToken() : "";
                                mPhotoUrl = removeScaleFromGooglePhotoUrl(
                                        account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "");
                            }

                            PushSocialPayload payload = new PushSocialPayload(mIdToken, SERVICE_TYPE_GOOGLE);
                            AppLog.d(T.MAIN,
                                    "GOOGLE SIGNUP: Google has returned a sign up result - dispatching SocialSignupAction");
                            mDispatcher.dispatch(AccountActionBuilder.newPushSocialSignupAction(payload));
                            mOldSitesIds = SiteUtils.getCurrentSiteIds(mSiteStore, false);
                        } catch (NullPointerException exception) {
                            AppLog.d(T.MAIN, "GOOGLE SIGNUP: Google has returned a sign up result - NPE");
                            AppLog.e(T.NUX, "Cannot get ID token from Google signup account.", exception);
                            showError(getString(R.string.login_error_generic));
                        }
                    } else {
                        AppLog.d(T.MAIN, "GOOGLE SIGNUP: Google has returned a sign up result - error");
                        mAnalyticsListener.trackSignupSocialButtonFailure();
                        switch (signInResult.getStatus().getStatusCode()) {
                            // Internal error.
                            case GoogleSignInStatusCodes.INTERNAL_ERROR:
                                AppLog.e(T.NUX, "Google Signup Failed: internal error.");
                                showError(getString(R.string.login_error_generic));
                                break;
                            // Attempted to connect with an invalid account name specified.
                            case GoogleSignInStatusCodes.INVALID_ACCOUNT:
                                AppLog.e(T.NUX, "Google Signup Failed: invalid account name.");
                                showError(getString(R.string.login_error_generic)
                                          + getString(R.string.login_error_suffix));
                                break;
                            // Network error.
                            case GoogleSignInStatusCodes.NETWORK_ERROR:
                                AppLog.e(T.NUX, "Google Signup Failed: network error.");
                                showError(getString(R.string.error_generic_network));
                                break;
                            // Cancelled by the user.
                            case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                                AppLog.e(T.NUX, "Google Signup Failed: cancelled by user.");
                                break;
                            // Attempt didn't succeed with the current account.
                            case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                                AppLog.e(T.NUX, "Google Signup Failed: current account failed.");
                                showError(getString(R.string.login_error_generic));
                                break;
                            // Attempted to connect, but the user is not signed in.
                            case GoogleSignInStatusCodes.SIGN_IN_REQUIRED:
                                AppLog.e(T.NUX, "Google Signup Failed: user is not signed in.");
                                showError(getString(R.string.login_error_generic));
                                break;
                            // Timeout error.
                            case GoogleSignInStatusCodes.TIMEOUT:
                                AppLog.e(T.NUX, "Google Signup Failed: timeout error.");
                                showError(getString(R.string.google_error_timeout));
                                break;
                            // Unknown error.
                            default:
                                AppLog.e(T.NUX, "Google Signup Failed: unknown error.");
                                showError(getString(R.string.login_error_generic));
                                break;
                        }
                    }
                } else if (result == RESULT_CANCELED) {
                    AppLog.d(T.MAIN, "GOOGLE SIGNUP: Google has returned a sign up result - canceled");
                    mAnalyticsListener.trackSignupSocialButtonFailure();
                    AppLog.e(T.NUX, "Google Signup Failed: result was CANCELED.");
                    finishSignUp();
                } else {
                    AppLog.d(T.MAIN, "GOOGLE SIGNUP: Google has returned a sign up result - unknown");
                    mAnalyticsListener.trackSignupSocialButtonFailure();
                    AppLog.e(T.NUX, "Google Signup Failed: result was not OK or CANCELED.");
                    showError(getString(R.string.login_error_generic));
                }

                break;
        }
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    // Remove scale from photo URL path string. Current URL matches /s96-c, which returns a 96 x 96
    // pixel image. Removing /s96-c from the string returns a 512 x 512 pixel image. Using regular
    // expressions may help if the photo URL scale value in the returned path changes.
    private String removeScaleFromGooglePhotoUrl(String photoUrl) {
        Pattern pattern = Pattern.compile("(/s[0-9]+-c)");
        Matcher matcher = pattern.matcher(photoUrl);
        return matcher.find() ? photoUrl.replace(matcher.group(1), "") : photoUrl;
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            AppLog.d(T.MAIN, "GOOGLE SIGNUP: onAuthenticationChanged - error");
            AppLog.e(T.API,
                    "SignupGoogleFragment.onAuthenticationChanged: " + event.error.type + " - " + event.error.message);
        } else if (event.createdAccount) {
            AppLog.d(T.MAIN,
                    "GOOGLE SIGNUP: onAuthenticationChanged - createdAccount=true -> the email is already attached to"
                    + " an wordpress account");
            mAnalyticsListener.trackCreatedAccount(event.userName, mGoogleEmail);
            mGoogleListener.onGoogleSignupFinished(mDisplayName, mGoogleEmail, mPhotoUrl, event.userName);
            // Continue with login since existing account was selected.
        } else {
            AppLog.d(T.MAIN, "GOOGLE SIGNUP: onAuthenticationChanged - new wordpress account created");
            mAnalyticsListener.trackSignupSocialToLogin();
            mLoginListener.loggedInViaSocialAccount(mOldSitesIds, true);
        }
        finishSignUp();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocialChanged(OnSocialChanged event) {
        if (event.isError()) {
            AppLog.e(T.API, "SignupGoogleFragment.onSocialChanged: " + event.error.type + " - " + event.error.message);

            switch (event.error.type) {
                // WordPress account exists with input email address, and two-factor authentication is required.
                case TWO_STEP_ENABLED:
                    AppLog.d(T.MAIN, "GOOGLE SIGNUP: onSocialChanged - error - two step authentication");
                    mAnalyticsListener.trackSignupSocialToLogin();
                    mLoginListener.showSignupToLoginMessage();
                    // Dispatch social login action to retrieve data required for two-factor authentication.
                    PushSocialPayload payload = new PushSocialPayload(mIdToken, SERVICE_TYPE_GOOGLE);
                    AppLog.d(T.MAIN,
                            "GOOGLE SIGNUP: onSocialChanged - error - two step authentication - dispatching pushSocialLoginAction");
                    mDispatcher.dispatch(AccountActionBuilder.newPushSocialLoginAction(payload));
                    break;
                // WordPress account exists with input email address, but not connected.
                case USER_EXISTS:
                    AppLog.d(T.MAIN, "GOOGLE SIGNUP: onSocialChanged - error - user already exists");
                    handleUserExists();
                    break;
                default:
                    AppLog.d(T.MAIN, "GOOGLE SIGNUP: onSocialChanged - error - unknown");
                    showError(getString(R.string.login_error_generic));
                    break;
            }
            // Response does not return error when two-factor authentication is required.
        } else if (event.requiresTwoStepAuth) {
            AppLog.d(T.MAIN, "GOOGLE SIGNUP: onSocialChanged - 2fa required");
            mAnalyticsListener.trackSignupSocialToLogin();
            mLoginListener.needs2faSocial(mGoogleEmail, event.userId, event.nonceAuthenticator, event.nonceBackup,
                    event.nonceSms);
            finishSignUp();
        } else {
            AppLog.d(T.MAIN, "GOOGLE SIGNUP: onSocialChanged - shouldn't happen - google sign in success");
           handleUserExists();
        }
    }

    private void handleUserExists() {
        mAnalyticsListener.trackSignupSocialAccountsNeedConnecting();
        mAnalyticsListener.trackSignupSocialToLogin();
        mLoginListener.showSignupToLoginMessage();
        mLoginListener.loginViaSocialAccount(mGoogleEmail, mIdToken, SERVICE_TYPE_GOOGLE, true);
        finishSignUp();
    }
}
