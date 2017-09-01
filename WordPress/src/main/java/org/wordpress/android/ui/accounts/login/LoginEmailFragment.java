package org.wordpress.android.ui.accounts.login;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAvailabilityChecked;
import org.wordpress.android.fluxc.store.AccountStore.OnSocialChanged;
import org.wordpress.android.fluxc.store.AccountStore.PushSocialLoginPayload;
import org.wordpress.android.ui.accounts.LoginMode;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.widgets.WPLoginInputRow;
import org.wordpress.android.widgets.WPLoginInputRow.OnEditorCommitListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.app.Activity.RESULT_OK;

public class LoginEmailFragment extends LoginBaseFormFragment<LoginListener>
        implements TextWatcher, OnEditorCommitListener, ConnectionCallbacks, OnConnectionFailedListener {
    private static final String KEY_REQUESTED_EMAIL = "KEY_REQUESTED_EMAIL";
    private static final String SERVICE_TYPE_GOOGLE = "google";
    private static final String STATE_RESOLVING_ERROR = "STATE_RESOLVING_ERROR";
    private static final int REQUEST_CONNECT = 1000;
    private static final int REQUEST_LOGIN = 1001;

    public static final String TAG = "login_email_fragment_tag";
    public static final int MAX_EMAIL_LENGTH = 100;

    private GoogleApiClient mGoogleApiClient;
    private String mGoogleEmail;
    private String mRequestedEmail;
    private WPLoginInputRow mEmailInput;
    private boolean isResolvingError;
    private boolean isThirdParty;
    private boolean shouldResolveError;

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.login_email_screen;
    }

    @Override
    protected @LayoutRes int getProgressBarText() {
        return R.string.checking_email;
    }

    @Override
    protected void setupLabel(TextView label) {
        switch (mLoginListener.getLoginMode()) {
            case WPCOM_LOGIN_DEEPLINK:
                label.setText(R.string.login_log_in_for_deeplink);
                break;
            case FULL:
                label.setText(R.string.enter_email_wordpress_com);
                break;
            case JETPACK_STATS:
                label.setText(R.string.stats_sign_in_jetpack_different_com_account);
                break;
            case WPCOM_REAUTHENTICATE:
                label.setText(R.string.auth_required);
                break;
        }
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        mEmailInput = (WPLoginInputRow) rootView.findViewById(R.id.login_email_row);
        autoFillFromBuildConfig("DEBUG_DOTCOM_LOGIN_EMAIL", mEmailInput.getEditText());
        mEmailInput.addTextChangedListener(this);
        mEmailInput.setOnEditorCommitListener(this);

        LinearLayout googleLoginLayout = (LinearLayout) rootView.findViewById(R.id.login_google_layout);
        googleLoginLayout.setVisibility(BuildConfig.LOGIN_GOOGLE_ENABLED ? View.VISIBLE : View.GONE);

        LinearLayout googleLoginButton = (LinearLayout) rootView.findViewById(R.id.login_google_button);
        googleLoginButton.setOnClickListener(new OnClickListener() {
            @SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
            @Override
            public void onClick(View view) {
                WPActivityUtils.hideKeyboard(getActivity().getCurrentFocus());

                // Start login process.
                if (NetworkUtils.checkConnection(getActivity()) && !isResolvingError) {
                    connectGoogleClient();
                    isThirdParty = true;
                }
            }
        });
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        secondaryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginListener != null) {
                    if (mLoginListener.getLoginMode() == LoginMode.JETPACK_STATS) {
                        mLoginListener.loginViaWpcomUsernameInstead();
                    } else {
                        mLoginListener.loginViaSiteAddress();
                    }
                }
            }
        });

        switch (mLoginListener.getLoginMode()) {
            case FULL:
                // all features enabled and with typical values
                secondaryButton.setText(R.string.enter_site_address_instead);
                break;
            case JETPACK_STATS:
                secondaryButton.setText(R.string.enter_username_instead);
                break;
            case WPCOM_LOGIN_DEEPLINK:
                secondaryButton.setVisibility(View.GONE);
                break;
            case WPCOM_REAUTHENTICATE:
                secondaryButton.setVisibility(View.GONE);
                break;
        }

        primaryButton.setOnClickListener(new OnClickListener() {
            @SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
            public void onClick(View v) {
                next(getCleanedEmail());
            }
        });
    }

    @Override
    protected EditText getEditTextToFocusOnStart() {
        return mEmailInput.getEditText();
    }

    @Override
    protected void onHelp() {
        if (mLoginListener != null) {
            if (isThirdParty) {
                // Send last email chosen from Google login if available
                mLoginListener.helpSocialEmailScreen(mGoogleEmail);
            } else {
                // Send exact string the user has inputted for email
                mLoginListener.helpEmailScreen(EditTextUtils.getText(mEmailInput.getEditText()));
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        // Restore state of error resolving.
        isResolvingError = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

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
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mRequestedEmail = savedInstanceState.getString(KEY_REQUESTED_EMAIL);
        } else {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_EMAIL_FORM_VIEWED);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_REQUESTED_EMAIL, mRequestedEmail);
        outState.putBoolean(STATE_RESOLVING_ERROR, isResolvingError);
    }

    protected void next(String email) {
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        if (isValidEmail(email)) {
            startProgress();
            mRequestedEmail = email;
            mDispatcher.dispatch(AccountActionBuilder.newIsAvailableEmailAction(email));
        } else {
            showEmailError(R.string.email_invalid);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginListener = null;
        disconnectGoogleClient();
    }

    @Override
    public void onConnected(Bundle bundle) {
        // Indicates account was selected, that account has granted any required permissions, and a
        // connection to Google Play services has been established.
        if (shouldResolveError) {
            shouldResolveError = false;
            showAccountDialog();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Could not connect to Google Play Services.  The user needs to select an account, grant
        // permissions or resolve an error in order to sign in.  Refer to the documentation for
        // ConnectionResult to see possible error codes.
        if (!isResolvingError && shouldResolveError) {
            if (connectionResult.hasResolution()) {
                try {
                    isResolvingError = true;
                    connectionResult.startResolutionForResult(getActivity(), REQUEST_CONNECT);
                } catch (IntentSender.SendIntentException exception) {
                    isResolvingError = false;
                    mGoogleApiClient.connect();
                }
            } else {
                isResolvingError = false;
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
        Log.w(LoginEmailFragment.class.getSimpleName(), "onConnectionSuspended: " + i);
    }

    private String getCleanedEmail() {
        return EditTextUtils.getText(mEmailInput.getEditText()).trim();
    }

    private boolean isValidEmail(String email) {
        Pattern emailRegExPattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = emailRegExPattern.matcher(email);

        return matcher.find() && email.length() <= MAX_EMAIL_LENGTH;
    }

    @Override
    public void OnEditorCommit() {
        next(getCleanedEmail());
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mEmailInput.setError(null);
        isThirdParty = false;
    }

    private void showEmailError(int messageId) {
        mEmailInput.setError(getString(messageId));
    }

    @Override
    protected void endProgress() {
        super.endProgress();
        mRequestedEmail = null;
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAvailabilityChecked(OnAvailabilityChecked event) {
        if (mRequestedEmail == null || !mRequestedEmail.equalsIgnoreCase(event.value)) {
            // bail if user canceled or a different email request is outstanding
            return;
        }

        if (isInProgress()) {
            endProgress();
        }

        if (event.isError()) {
            // report the error but don't bail yet.
            AppLog.e(T.API, "OnAvailabilityChecked has error: " + event.error.type + " - " + event.error.message);
            showEmailError(R.string.email_not_registered_wpcom);
            return;
        }

        switch (event.type) {
            case EMAIL:
                if (event.isAvailable) {
                    // email address is available on wpcom, so apparently the user can't login with that one.
                    showEmailError(R.string.email_not_registered_wpcom);
                } else if (mLoginListener != null) {
                    EditTextUtils.hideSoftInput(mEmailInput.getEditText());
                    mLoginListener.gotWpcomEmail(event.value);
                }
                break;
            default:
                AppLog.e(T.API, "OnAvailabilityChecked unhandled event type: " + event.error.type);
                break;
        }
    }

    public void connectGoogleClient() {
        if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
            shouldResolveError = true;
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
                    shouldResolveError = false;
                }

                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                } else {
                    showAccountDialog();
                }

                isResolvingError = false;
                break;
            case REQUEST_LOGIN:
                if (result == RESULT_OK) {
                    GoogleSignInResult signInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

                    if (signInResult.isSuccess()) {
                        try {
                            GoogleSignInAccount account = signInResult.getSignInAccount();
                            mGoogleEmail = account.getEmail();
                            PushSocialLoginPayload payload = new PushSocialLoginPayload(account.getIdToken(), SERVICE_TYPE_GOOGLE);
                            mDispatcher.dispatch(AccountActionBuilder.newPushSocialLoginAction(payload));
                        } catch (NullPointerException exception) {
                            disconnectGoogleClient();
                            AppLog.e(T.NUX, "Cannot get ID token from Google sign-in account.", exception);
                            showErrorDialog(getString(R.string.login_error_generic));
                        }
                    } else {
                        switch (signInResult.getStatus().getStatusCode()) {
                            // Internal error.
                            case GoogleSignInStatusCodes.INTERNAL_ERROR:
                                AppLog.e(AppLog.T.NUX, "Google Sign-in Failed: internal error.");
                                showErrorDialog(getString(R.string.login_error_generic)
                                        + getString(R.string.login_error_suffix_internal));
                                break;
                            // Attempted to connect with an invalid account name specified.
                            case GoogleSignInStatusCodes.INVALID_ACCOUNT:
                                AppLog.e(AppLog.T.NUX, "Google Sign-in Failed: invalid account name.");
                                showErrorDialog(getString(R.string.login_error_generic)
                                        + getString(R.string.login_error_suffix_invalid));
                                break;
                            // Network error.
                            case GoogleSignInStatusCodes.NETWORK_ERROR:
                                AppLog.e(AppLog.T.NUX, "Google Sign-in Failed: network error.");
                                showErrorDialog(getString(R.string.error_generic_network));
                                break;
                            // Cancelled by the user.
                            case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                                AppLog.e(AppLog.T.NUX, "Google Sign-in Failed: cancelled by user.");
                                break;
                            // Attempt didn't succeed with the current account.
                            case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                                AppLog.e(AppLog.T.NUX, "Google Sign-in Failed: current account failed.");
                            // Attempted to connect, but the user is not signed in.
                            case GoogleSignInStatusCodes.SIGN_IN_REQUIRED:
                                AppLog.e(AppLog.T.NUX, "Google Sign-in Failed: user is not signed in.");
                            // Unknown error.
                            default:
                                showErrorDialog(getString(R.string.login_error_generic));
                                break;
                        }
                    }
                } else {
                    AppLog.e(AppLog.T.NUX, "Google Sign-in Failed: result was not RESULT_OK.");
                    showErrorDialog(getString(R.string.login_error_generic));
                }

                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            disconnectGoogleClient();
            AppLog.e(T.API, "LoginEmailFragment.onAuthenticationChanged: " + event.error.type + " - " + event.error.message);
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FAILED, event.getClass().getSimpleName(),
                    event.error.type.toString(), event.error.message);
            showErrorDialog(getString(R.string.login_error_generic));
        } else {
            AppLog.i(T.NUX, "LoginEmailFragment.onAuthenticationChanged: " + event.toString());
            doFinishLogin();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocialChanged(OnSocialChanged event) {
        if (event.isError()) {
            disconnectGoogleClient();
            AppLog.e(T.API, "LoginEmailFragment.onSocialChanged: " + event.error.type + " - " + event.error.message);
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FAILED, event.getClass().getSimpleName(),
                    event.error.type.toString(), event.error.message);

            switch (event.error.type) {
                // WordPress account does not exist with input email address.
                case UNKNOWN_USER:
                    showErrorDialog(getString(R.string.login_error_email_not_found, mGoogleEmail));
                    break;
                // Unknown error.
                case GENERIC_ERROR:
                    // Do nothing for now (included to show all error types) and just fall through to 'default'
                default:
                    showErrorDialog(getString(R.string.login_error_generic));
                    break;
            }
        }
    }
}
