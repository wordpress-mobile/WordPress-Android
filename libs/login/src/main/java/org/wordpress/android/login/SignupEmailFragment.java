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
import org.wordpress.android.login.widgets.WPLoginInputRow;
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dagger.android.support.AndroidSupportInjection;

import static android.app.Activity.RESULT_OK;

public class SignupEmailFragment extends LoginBaseFormFragment<LoginListener> implements TextWatcher,
        OnEditorCommitListener, ConnectionCallbacks, OnConnectionFailedListener {
    private static final String KEY_HAS_DISMISSED_EMAIL_HINTS = "KEY_HAS_DISMISSED_EMAIL_HINTS";
    private static final String KEY_IS_DISPLAYING_EMAIL_HINTS = "KEY_IS_DISPLAYING_EMAIL_HINTS";
    private static final String KEY_REQUESTED_EMAIL = "KEY_REQUESTED_EMAIL";
    private static final String LOG_TAG = SignupEmailFragment.class.getSimpleName();
    private static final int EMAIL_CREDENTIALS_REQUEST_CODE = 25100;
    private static final int GOOGLE_API_CLIENT_ID = 1001;

    public static final String TAG = "signup_email_fragment_tag";
    public static final int MAX_EMAIL_LENGTH = 100;

    private GoogleApiClient mGoogleApiClient;
    private String mRequestedEmail;

    protected Button mPrimaryButton;
    protected WPLoginInputRow mEmailInput;
    protected boolean mHasDismissedEmailHints;
    protected boolean mIsDisplayingEmailHints;

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.signup_email_fragment;
    }

    @Override
    protected @LayoutRes int getProgressBarText() {
        return R.string.checking_email;
    }

    @Override
    protected void setupLabel(@NonNull TextView label) {
        label.setText(R.string.signup_email_header);
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
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        secondaryButton.setVisibility(View.GONE);

        mPrimaryButton = primaryButton;
        mPrimaryButton.setEnabled(false);
        mPrimaryButton.setOnClickListener(new OnClickListener() {
            @SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
            public void onClick(View view) {
                next(getCleanedEmail());
            }
        });
    }

    @Override
    protected void onHelp() {
        if (mLoginListener != null) {
            mLoginListener.helpSignupEmailScreen(EditTextUtils.getText(mEmailInput.getEditText()));
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
                .addConnectionCallbacks(SignupEmailFragment.this)
                .enableAutoManage(getActivity(), GOOGLE_API_CLIENT_ID, SignupEmailFragment.this)
                .addApi(Auth.CREDENTIALS_API)
                .build();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mRequestedEmail = savedInstanceState.getString(KEY_REQUESTED_EMAIL);
            mIsDisplayingEmailHints = savedInstanceState.getBoolean(KEY_IS_DISPLAYING_EMAIL_HINTS);
            mHasDismissedEmailHints = savedInstanceState.getBoolean(KEY_HAS_DISMISSED_EMAIL_HINTS);
        } else {
            mAnalyticsListener.trackEmailFormViewed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_REQUESTED_EMAIL, mRequestedEmail);
        outState.putBoolean(KEY_IS_DISPLAYING_EMAIL_HINTS, mIsDisplayingEmailHints);
        outState.putBoolean(KEY_HAS_DISMISSED_EMAIL_HINTS, mHasDismissedEmailHints);
    }

    protected void next(String email) {
        if (NetworkUtils.checkConnection(getActivity())) {
            if (isValidEmail(email)) {
                startProgress();
                mRequestedEmail = email;
                mDispatcher.dispatch(AccountActionBuilder.newIsAvailableEmailAction(email));
            } else {
                showErrorEmail(getString(R.string.email_invalid));
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginListener = null;

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.stopAutoManage(getActivity());
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
        mPrimaryButton.setEnabled(!s.toString().trim().isEmpty());
    }

    protected void showErrorDialog(String message) {
        AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.LoginTheme))
                .setMessage(message)
                .setPositiveButton(R.string.login_error_button, null)
                .create();
        dialog.show();
    }

    private void showErrorEmail(String message) {
        mEmailInput.setError(message);
    }

    @Override
    protected void endProgress() {
        super.endProgress();
        mRequestedEmail = null;
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAvailabilityChecked(OnAvailabilityChecked event) {
        if (mRequestedEmail != null && mRequestedEmail.equalsIgnoreCase(event.value)) {
            if (isInProgress()) {
                endProgress();
            }

            if (event.isError()) {
                AppLog.e(T.API, "OnAvailabilityChecked error: " + event.error.type + " - " + event.error.message);
                showErrorDialog(getString(R.string.signup_email_error_generic));
            } else {
                switch (event.type) {
                    case EMAIL:
                        ActivityUtils.hideKeyboard(getActivity());

                        if (mLoginListener != null) {
                            if (event.isAvailable) {
                                mLoginListener.showSignupMagicLink(event.value);
                            } else {
                                mAnalyticsListener.trackAnalyticsSignIn(mAccountStore, mSiteStore, true);
                                mLoginListener.showSignupToLoginMessage();
                                mLoginListener.gotWpcomEmail(event.value);
                                // Kill connections with FluxC and this fragment since the flow is changing to login.
                                mDispatcher.unregister(this);
                                getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
                            }
                        }

                        break;
                    default:
                        AppLog.e(T.API, "OnAvailabilityChecked unhandled event: " + event.error.type);
                        break;
                }
            }
        }
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
                            EditTextUtils.showSoftInput(mEmailInput.getEditText());
                        }
                    }
                }, getResources().getInteger(android.R.integer.config_mediumAnimTime));
            }

            mIsDisplayingEmailHints = false;
        }
    }
}
