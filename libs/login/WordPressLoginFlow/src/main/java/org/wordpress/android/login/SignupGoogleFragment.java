package org.wordpress.android.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

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

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static org.wordpress.android.login.LoginAnalyticsListener.CreatedAccountSource.GOOGLE;

import dagger.android.support.AndroidSupportInjection;

public class SignupGoogleFragment extends GoogleFragment {
    private static final String OLD_SITES_IDS = "old_sites_ids";
    private static final String SIGN_UP_REQUESTED = "sign_up_requested";

    private static final String ARG_GOOGLE_EMAIL = "ARG_GOOGLE_EMAIL";
    private static final String ARG_DISPLAY_NAME = "ARG_DISPLAY_NAME";
    private static final String ARG_ID_TOKEN = "ARG_ID_TOKEN";
    private static final String ARG_PHOTO_URL = "ARG_PHOTO_URL";
    private static final String ARG_FORCE_SIGNUP_AT_START = "ARG_FORCE_SIGNUP_AT_START";

    private ArrayList<Integer> mOldSitesIds;
    private ProgressDialog mProgressDialog;
    private boolean mSignupRequested;
    private boolean mForceSignupAtStart;

    private static final int REQUEST_SIGNUP = 1002;

    public static final String TAG = "signup_google_fragment_tag";

    public static SignupGoogleFragment newInstance(String email, String displayName, String idToken, String photoUrl) {
        SignupGoogleFragment fragment = new SignupGoogleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GOOGLE_EMAIL, email);
        args.putString(ARG_DISPLAY_NAME, displayName);
        args.putString(ARG_ID_TOKEN, idToken);
        args.putString(ARG_PHOTO_URL, photoUrl);
        args.putBoolean(ARG_FORCE_SIGNUP_AT_START, true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        mProgressDialog = ProgressDialog.show(
                getActivity(), null, getString(R.string.signup_with_google_progress), true, false, null);
        super.onAttach(context);
        Bundle args = getArguments();
        if (args != null) {
            mDisplayName = args.getString(ARG_DISPLAY_NAME);
            mGoogleEmail = args.getString(ARG_GOOGLE_EMAIL);
            mIdToken = args.getString(ARG_ID_TOKEN);
            mPhotoUrl = args.getString(ARG_PHOTO_URL);
            mForceSignupAtStart = args.getBoolean(ARG_FORCE_SIGNUP_AT_START);
        }
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
    protected void startFlow() {
        if (mForceSignupAtStart) {
            dispatchSocialSignup(mIdToken);
        } else {
            if (!mSignupRequested) {
                AppLog.d(T.MAIN, "GOOGLE SIGNUP: startFlow");
                mSignupRequested = true;
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, REQUEST_SIGNUP);
            } else {
                AppLog.d(T.MAIN, "GOOGLE SIGNUP: startFlow called, but is already in progress");
            }
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
                        AppLog.d(T.MAIN, "GOOGLE SIGNUP: sign up result returned - succcess");
                        try {
                            GoogleSignInAccount account = signInResult.getSignInAccount();

                            if (account != null) {
                                mDisplayName = account.getDisplayName() != null ? account.getDisplayName() : "";
                                mGoogleEmail = account.getEmail() != null ? account.getEmail() : "";
                                mIdToken = account.getIdToken() != null ? account.getIdToken() : "";
                                mPhotoUrl = removeScaleFromGooglePhotoUrl(
                                        account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "");
                            }

                            AppLog.d(T.MAIN, "GOOGLE SIGNUP: sign up result returned - dispatching SocialSignupAction");

                            dispatchSocialSignup(mIdToken);
                        } catch (NullPointerException exception) {
                            AppLog.d(T.MAIN, "GOOGLE SIGNUP: sign up result returned - NPE");
                            AppLog.e(T.NUX, "Cannot get ID token from Google signup account.", exception);
                            showError(getString(R.string.login_error_generic));
                        }
                    } else {
                        AppLog.d(T.MAIN, "GOOGLE SIGNUP: sign up result returned - error");
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
                    AppLog.d(T.MAIN, "GOOGLE SIGNUP: sign up result returned - canceled");
                    mAnalyticsListener.trackSignupSocialButtonFailure();
                    AppLog.e(T.NUX, "Google Signup Failed: result was CANCELED.");
                    finishFlow();
                } else {
                    AppLog.d(T.MAIN, "GOOGLE SIGNUP: sign up result returned - unknown");
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

    private void dispatchSocialSignup(String idToken) {
        PushSocialPayload payload = new PushSocialPayload(idToken, SERVICE_TYPE_GOOGLE);
        mDispatcher.dispatch(AccountActionBuilder.newPushSocialSignupAction(payload));
        mOldSitesIds = SiteUtils.getCurrentSiteIds(mSiteStore, false);
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
                    "GOOGLE SIGNUP: onAuthenticationChanged - new wordpress account created");
            mAnalyticsListener.trackCreatedAccount(event.userName, mGoogleEmail, GOOGLE);
            mAnalyticsListener.trackAnalyticsSignIn(true);
            mGoogleListener.onGoogleSignupFinished(mDisplayName, mGoogleEmail, mPhotoUrl, event.userName);
            // Continue with login since existing account was selected.
        } else {
            AppLog.d(T.MAIN, "GOOGLE SIGNUP: onAuthenticationChanged - the email is already attached to an account");
            mAnalyticsListener.trackSignupSocialToLogin();
            mLoginListener.loggedInViaSocialAccount(mOldSitesIds, true);
        }
        finishFlow();
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
                            "GOOGLE SIGNUP: onSocialChanged error - two step authentication - dispatching "
                            + "pushSocialLoginAction");
                    mDispatcher.dispatch(AccountActionBuilder.newPushSocialLoginAction(payload));
                    break;
                // WordPress account exists with input email address, but not connected.
                case USER_EXISTS:
                    AppLog.d(T.MAIN, "GOOGLE SIGNUP: onSocialChanged - error - user already exists");
                    loginViaSocialAccount();
                    break;
                // Too many attempts on sending SMS verification code. The user has to wait before they try again
                case SMS_CODE_THROTTLED:
                    AppLog.d(T.MAIN, "GOOGLE SIGNUP: onSocialChanged - error - sms code throttled");
                    showError(getString(R.string.login_error_sms_throttled));
                    break;
                default:
                    AppLog.d(T.MAIN, "GOOGLE SIGNUP: onSocialChanged - error - unknown");
                    showError(getString(R.string.login_error_generic));
                    break;
            }
        // Response does not return error when two-factor authentication is required.
        } else if (event.requiresTwoStepAuth || Login2FaFragment.TWO_FACTOR_TYPE_SMS.equals(event.notificationSent)) {
            AppLog.d(T.MAIN, "GOOGLE SIGNUP: onSocialChanged - 2fa required");
            mAnalyticsListener.trackSignupSocialToLogin();
            mLoginListener.needs2faSocial(mGoogleEmail, event.userId, event.nonceAuthenticator, event.nonceBackup,
                    event.nonceSms);
            finishFlow();
        } else {
            AppLog.d(T.MAIN, "GOOGLE SIGNUP: onSocialChanged - google login success");
           loginViaSocialAccount();
        }
    }

    private void loginViaSocialAccount() {
        mAnalyticsListener.trackSignupSocialAccountsNeedConnecting();
        mAnalyticsListener.trackSignupSocialToLogin();
        mLoginListener.showSignupToLoginMessage();
        mLoginListener.loginViaSocialAccount(mGoogleEmail, mIdToken, SERVICE_TYPE_GOOGLE, true);
        finishFlow();
    }
}
