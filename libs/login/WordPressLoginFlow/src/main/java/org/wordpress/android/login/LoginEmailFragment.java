package org.wordpress.android.login;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore.OnAvailabilityChecked;
import org.wordpress.android.login.util.SiteUtils;
import org.wordpress.android.login.widgets.WPLoginInputRow;
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dagger.android.support.AndroidSupportInjection;

import static android.app.Activity.RESULT_OK;

public class LoginEmailFragment extends LoginBaseFormFragment<LoginListener> implements TextWatcher,
        OnEditorCommitListener, ConnectionCallbacks, OnConnectionFailedListener {
    private static final String KEY_GOOGLE_EMAIL = "KEY_GOOGLE_EMAIL";
    private static final String KEY_HAS_DISMISSED_EMAIL_HINTS = "KEY_HAS_DISMISSED_EMAIL_HINTS";
    private static final String KEY_IS_DISPLAYING_EMAIL_HINTS = "KEY_IS_DISPLAYING_EMAIL_HINTS";
    private static final String KEY_IS_SOCIAL = "KEY_IS_SOCIAL";
    private static final String KEY_OLD_SITES_IDS = "KEY_OLD_SITES_IDS";
    private static final String KEY_REQUESTED_EMAIL = "KEY_REQUESTED_EMAIL";
    private static final String LOG_TAG = LoginEmailFragment.class.getSimpleName();
    private static final int GOOGLE_API_CLIENT_ID = 1001;
    private static final int EMAIL_CREDENTIALS_REQUEST_CODE = 25100;

    public static final String TAG = "login_email_fragment_tag";
    public static final int MAX_EMAIL_LENGTH = 100;

    private ArrayList<Integer> mOldSitesIDs;
    private GoogleApiClient mGoogleApiClient;
    private String mGoogleEmail;
    private String mRequestedEmail;
    private boolean mIsSocialLogin;

    protected WPLoginInputRow mEmailInput;
    protected boolean mHasDismissedEmailHints;
    protected boolean mIsDisplayingEmailHints;

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.login_email_screen;
    }

    @Override
    protected @LayoutRes int getProgressBarText() {
        return mIsSocialLogin ? R.string.logging_in : R.string.checking_email;
    }

    @Override
    protected void setupLabel(@NonNull TextView label) {
        switch (mLoginListener.getLoginMode()) {
            case WPCOM_LOGIN_DEEPLINK:
                label.setText(R.string.login_log_in_for_deeplink);
                break;
            case SHARE_INTENT:
                label.setText(R.string.login_log_in_for_share_intent);
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
        mEmailInput = rootView.findViewById(R.id.login_email_row);
        if (BuildConfig.DEBUG) {
            mEmailInput.getEditText().setText(BuildConfig.DEBUG_WPCOM_LOGIN_EMAIL);
        }
        mEmailInput.addTextChangedListener(this);
        mEmailInput.setOnEditorCommitListener(this);
        mEmailInput.getEditText().setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus && !mIsDisplayingEmailHints && !mHasDismissedEmailHints) {
                    mIsDisplayingEmailHints = true;
                    getEmailHints();
                }
            }
        });
        mEmailInput.getEditText().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mIsDisplayingEmailHints && !mHasDismissedEmailHints) {
                    mIsDisplayingEmailHints = true;
                    getEmailHints();
                }
            }
        });

        LinearLayout googleLoginButton = rootView.findViewById(R.id.login_google_button);
        googleLoginButton.setOnClickListener(new OnClickListener() {
            @SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
            @Override
            public void onClick(View view) {
                mAnalyticsListener.trackSocialButtonClick();
                ActivityUtils.hideKeyboardForced(mEmailInput.getEditText());

                if (NetworkUtils.checkConnection(getActivity())) {
                    if (isAdded()) {
                        mOldSitesIDs = SiteUtils.getCurrentSiteIds(mSiteStore, false);
                        mIsSocialLogin = true;
                        mLoginListener.addGoogleLoginFragment(LoginEmailFragment.this);
                    } else {
                        AppLog.e(T.NUX, "Google login could not be started.  LoginEmailFragment was not attached.");
                        showErrorDialog(getString(R.string.login_error_generic_start));
                    }
                }
            }
        });

        LinearLayout siteLoginButton = rootView.findViewById(R.id.login_site_button);
        siteLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLoginListener != null) {
                    if (mLoginListener.getLoginMode() == LoginMode.JETPACK_STATS) {
                        mLoginListener.loginViaWpcomUsernameInstead();
                    } else {
                        mLoginListener.loginViaSiteAddress();
                    }
                }
            }
        });

        ImageView siteLoginButtonIcon = rootView.findViewById(R.id.login_site_button_icon);
        TextView siteLoginButtonText = rootView.findViewById(R.id.login_site_button_text);

        switch (mLoginListener.getLoginMode()) {
            case FULL:
            case SHARE_INTENT:
                siteLoginButtonIcon.setImageResource(R.drawable.ic_domains_grey_24dp);
                siteLoginButtonText.setText(R.string.enter_site_address_instead);
                break;
            case JETPACK_STATS:
                siteLoginButtonIcon.setImageResource(R.drawable.ic_user_circle_grey_24dp);
                siteLoginButtonText.setText(R.string.enter_username_instead);
                break;
            case WPCOM_LOGIN_DEEPLINK:
            case WPCOM_REAUTHENTICATE:
                siteLoginButton.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        secondaryButton.setVisibility(View.GONE);

        primaryButton.setOnClickListener(new OnClickListener() {
            @SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
            public void onClick(View v) {
                next(getCleanedEmail());
            }
        });
    }

    @Override
    protected void onHelp() {
        if (mLoginListener != null) {
            if (mIsSocialLogin) {
                // Send last email chosen from Google login if available.
                mLoginListener.helpSocialEmailScreen(mGoogleEmail);
            } else {
                // Send exact string the user has inputted for email
                mLoginListener.helpEmailScreen(EditTextUtils.getText(mEmailInput.getEditText()));
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(LoginEmailFragment.this)
                .enableAutoManage(getActivity(), GOOGLE_API_CLIENT_ID, LoginEmailFragment.this)
                .addApi(Auth.CREDENTIALS_API)
                .build();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mOldSitesIDs = savedInstanceState.getIntegerArrayList(KEY_OLD_SITES_IDS);
            mRequestedEmail = savedInstanceState.getString(KEY_REQUESTED_EMAIL);
            mGoogleEmail = savedInstanceState.getString(KEY_GOOGLE_EMAIL);
            mIsSocialLogin = savedInstanceState.getBoolean(KEY_IS_SOCIAL);
            mIsDisplayingEmailHints = savedInstanceState.getBoolean(KEY_IS_DISPLAYING_EMAIL_HINTS);
            mHasDismissedEmailHints = savedInstanceState.getBoolean(KEY_HAS_DISMISSED_EMAIL_HINTS);
        } else {
            mAnalyticsListener.trackEmailFormViewed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList(KEY_OLD_SITES_IDS, mOldSitesIDs);
        outState.putString(KEY_REQUESTED_EMAIL, mRequestedEmail);
        outState.putString(KEY_GOOGLE_EMAIL, mGoogleEmail);
        outState.putBoolean(KEY_IS_SOCIAL, mIsSocialLogin);
        outState.putBoolean(KEY_IS_DISPLAYING_EMAIL_HINTS, mIsDisplayingEmailHints);
        outState.putBoolean(KEY_HAS_DISMISSED_EMAIL_HINTS, mHasDismissedEmailHints);
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

        mGoogleApiClient.stopAutoManage(getActivity());
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
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
    public void onEditorCommit() {
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
        mIsSocialLogin = false;
    }

    private void showEmailError(int messageId) {
        mEmailInput.setError(getString(messageId));
    }

    private void showErrorDialog(String message) {
        AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.LoginTheme))
                .setMessage(message)
                .setPositiveButton(R.string.login_error_button, null)
                .create();
        dialog.show();
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
            // hide the keyboard to ensure the link to login using the site address is visible
            ActivityUtils.hideKeyboardForced(mEmailInput);
            showEmailError(R.string.email_not_registered_wpcom);
            return;
        }

        switch (event.type) {
            case EMAIL:
                if (event.isAvailable) {
                    // email address is available on wpcom, so apparently the user can't login with that one.
                    ActivityUtils.hideKeyboardForced(mEmailInput);
                    showEmailError(R.string.email_not_registered_wpcom);
                } else if (mLoginListener != null) {
                    ActivityUtils.hideKeyboardForced(mEmailInput);
                    mLoginListener.gotWpcomEmail(event.value);
                }
                break;
            default:
                AppLog.e(T.API, "OnAvailabilityChecked unhandled event type: " + event.error.type);
                break;
        }
    }

    public void setGoogleEmail(String email) {
        mGoogleEmail = email;
    }

    public void finishLogin() {
        doFinishLogin();
    }

    @Override
    protected void onLoginFinished() {
        mAnalyticsListener.trackAnalyticsSignIn(mAccountStore, mSiteStore, true);
        mLoginListener.loggedInViaSocialAccount(mOldSitesIDs, false);
    }

    @Override
    public void onConnected(Bundle bundle) {
        AppLog.d(T.NUX, LOG_TAG + ": Google API client connected");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        AppLog.d(T.NUX, LOG_TAG + ": Google API connection result: " + connectionResult);
    }

    @Override
    public void onConnectionSuspended(int i) {
        AppLog.d(T.NUX, LOG_TAG + ": Google API client connection suspended");
    }

    public void getEmailHints() {
        HintRequest hintRequest = new HintRequest.Builder()
                .setHintPickerConfig(new CredentialPickerConfig.Builder()
                        .setShowCancelButton(true)
                        .build())
                .setEmailAddressIdentifierSupported(true)
                .build();

        PendingIntent intent = Auth.CredentialsApi.getHintPickerIntent(mGoogleApiClient, hintRequest);

        try {
            startIntentSenderForResult(intent.getIntentSender(), EMAIL_CREDENTIALS_REQUEST_CODE, null, 0, 0, 0, null);
        } catch (IntentSender.SendIntentException exception) {
            AppLog.d(T.NUX, LOG_TAG + "Could not start email hint picker" + exception);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EMAIL_CREDENTIALS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                mEmailInput.getEditText().setText(credential.getId());
                next(getCleanedEmail());
            } else {
                mHasDismissedEmailHints = true;
                mEmailInput.getEditText().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isAdded()) {
                            ActivityUtils.showKeyboard(mEmailInput.getEditText());
                        }
                    }
                }, getResources().getInteger(android.R.integer.config_mediumAnimTime));
            }

            mIsDisplayingEmailHints = false;
        }
    }
}
