package org.wordpress.android.login;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAvailabilityChecked;
import org.wordpress.android.login.SignupBottomSheetDialogFragment.SignupSheetListener;
import org.wordpress.android.login.util.ContextExtensionsKt;
import org.wordpress.android.login.util.SiteUtils;
import org.wordpress.android.login.widgets.WPLoginInputRow;
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.NetworkUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.app.Activity.RESULT_OK;

import dagger.android.support.AndroidSupportInjection;

public class LoginEmailFragment extends LoginBaseFormFragment<LoginListener> implements TextWatcher,
        OnEditorCommitListener, ConnectionCallbacks, OnConnectionFailedListener {
    private static final String KEY_GOOGLE_EMAIL = "KEY_GOOGLE_EMAIL";
    private static final String KEY_HAS_DISMISSED_EMAIL_HINTS = "KEY_HAS_DISMISSED_EMAIL_HINTS";
    private static final String KEY_IS_DISPLAYING_EMAIL_HINTS = "KEY_IS_DISPLAYING_EMAIL_HINTS";
    private static final String KEY_IS_SOCIAL = "KEY_IS_SOCIAL";
    private static final String KEY_OLD_SITES_IDS = "KEY_OLD_SITES_IDS";
    private static final String KEY_REQUESTED_EMAIL = "KEY_REQUESTED_EMAIL";
    private static final String KEY_EMAIL_ERROR_RES = "KEY_EMAIL_ERROR_RES";
    private static final String LOG_TAG = LoginEmailFragment.class.getSimpleName();
    private static final int GOOGLE_API_CLIENT_ID = 1002;
    private static final int EMAIL_CREDENTIALS_REQUEST_CODE = 25100;

    private static final String ARG_LOGIN_SITE_URL = "ARG_LOGIN_SITE_URL";
    private static final String ARG_SIGNUP_FROM_LOGIN_ENABLED = "ARG_SIGNUP_FROM_LOGIN_ENABLED";
    private static final String ARG_SITE_LOGIN_ENABLED = "ARG_SITE_LOGIN_ENABLED";
    private static final String ARG_SHOULD_USE_NEW_LAYOUT = "ARG_SHOULD_USE_NEW_LAYOUT";

    public static final String TAG = "login_email_fragment_tag";
    public static final int MAX_EMAIL_LENGTH = 100;

    private ArrayList<Integer> mOldSitesIDs = new ArrayList<>();
    private GoogleApiClient mGoogleApiClient;
    private String mGoogleEmail;
    private String mRequestedEmail;
    private boolean mIsSocialLogin;
    private Integer mCurrentEmailErrorRes = null;
    private boolean mIsSignupFromLoginEnabled;
    private boolean mIsSiteLoginEnabled;
    private boolean mShouldUseNewLayout;

    protected WPLoginInputRow mEmailInput;
    protected boolean mHasDismissedEmailHints;
    protected boolean mIsDisplayingEmailHints;
    protected String mLoginSiteUrl;

    public static LoginEmailFragment newInstance(String url) {
        LoginEmailFragment fragment = new LoginEmailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LOGIN_SITE_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    public static LoginEmailFragment newInstance(boolean isSignupFromLoginEnabled, boolean isSiteLoginEnabled,
                                                 boolean shouldUseNewLayout) {
        LoginEmailFragment fragment = new LoginEmailFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SIGNUP_FROM_LOGIN_ENABLED, isSignupFromLoginEnabled);
        args.putBoolean(ARG_SITE_LOGIN_ENABLED, isSiteLoginEnabled);
        args.putBoolean(ARG_SHOULD_USE_NEW_LAYOUT, shouldUseNewLayout);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected @LayoutRes int getContentLayout() {
        return mShouldUseNewLayout ? R.layout.login_email_screen : R.layout.login_email_screen_old;
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
            case WPCOM_LOGIN_ONLY:
                if (mShouldUseNewLayout) {
                    label.setText(R.string.enter_email_to_continue_wordpress_com);
                } else {
                    label.setText(R.string.enter_email_wordpress_com);
                }
                break;
            case WOO_LOGIN_MODE:
                label.setText(getString(R.string.enter_email_for_site, mLoginSiteUrl));
                break;
            case JETPACK_STATS:
                label.setText(R.string.login_to_to_connect_jetpack);
                break;
            case WPCOM_REAUTHENTICATE:
                label.setText(R.string.auth_required);
                break;
        }
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        // important for accessibility - talkback
        getActivity().setTitle(R.string.email_address_login_title);

        setupEmailInput((WPLoginInputRow) rootView.findViewById(R.id.login_email_row));

        if (mShouldUseNewLayout) {
            setupContinueButton((Button) rootView.findViewById(R.id.login_continue_button));
            setupTosButtons(
                    (Button) rootView.findViewById(R.id.continue_tos),
                    (Button) rootView.findViewById(R.id.continue_with_google_tos));
            setupSocialButtons((Button) rootView.findViewById(R.id.continue_with_google));
        } else {
            setupAlternativeButtons(
                    (LinearLayout) rootView.findViewById(R.id.login_google_button),
                    (LinearLayout) rootView.findViewById(R.id.login_site_button));
        }
    }

    private void setupEmailInput(WPLoginInputRow emailInput) {
        mEmailInput = emailInput;
        if (BuildConfig.DEBUG) {
            mEmailInput.getEditText().setText(BuildConfig.DEBUG_WPCOM_LOGIN_EMAIL);
        }
        mEmailInput.post(new Runnable() {
            @Override public void run() {
                mEmailInput.addTextChangedListener(LoginEmailFragment.this);
            }
        });

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

    private void setupContinueButton(Button continueButton) {
        continueButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                onContinueClicked();
            }
        });
    }

    private void setupTosButtons(Button continueTosButton, Button continueWithGoogleTosButton) {
        OnClickListener onClickListener = new OnClickListener() {
            public void onClick(View view) {
                Context context = getContext();
                if ((context instanceof SignupSheetListener)) {
                    ((SignupSheetListener) context).onSignupSheetTermsOfServiceClicked();
                }
            }
        };

        continueTosButton.setOnClickListener(onClickListener);
        continueTosButton.setText(formatTosText(R.string.continue_terms_of_service_text));

        continueWithGoogleTosButton.setOnClickListener(onClickListener);
        continueWithGoogleTosButton.setText(formatTosText(R.string.continue_with_google_terms_of_service_text));
    }

    private void setupSocialButtons(Button continueWithGoogleButton) {
        continueWithGoogleButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onGoogleSigninClicked();
            }
        });
    }

    private void setupAlternativeButtons(LinearLayout googleLoginButton, LinearLayout siteLoginButton) {
        googleLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onGoogleSigninClicked();
            }
        });

        siteLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLoginListener != null) {
                    LoginMode loginMode = mLoginListener.getLoginMode();
                    if (loginMode == LoginMode.JETPACK_STATS) {
                        mLoginListener.loginViaWpcomUsernameInstead();
                    } else if (loginMode == LoginMode.WOO_LOGIN_MODE) {
                        mLoginListener.loginViaSiteCredentials(mLoginSiteUrl);
                    } else {
                        mLoginListener.loginViaSiteAddress();
                    }
                }
            }
        });

        ImageView siteLoginButtonIcon = siteLoginButton.findViewById(R.id.login_site_button_icon);
        TextView siteLoginButtonText = siteLoginButton.findViewById(R.id.login_site_button_text);

        switch (mLoginListener.getLoginMode()) {
            case WOO_LOGIN_MODE:
                siteLoginButtonIcon.setImageResource(R.drawable.ic_domains_grey_24dp);
                siteLoginButtonText.setText(R.string.enter_site_credentials_instead);
                break;
            case FULL:
            case WPCOM_LOGIN_ONLY:
            case SHARE_INTENT:
                siteLoginButton.setVisibility(mIsSiteLoginEnabled ? View.VISIBLE : View.GONE);
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
        if (mShouldUseNewLayout) {
            secondaryButton.setVisibility(View.GONE);
            primaryButton.setVisibility(View.GONE);
        } else {
            setupSecondaryButton(secondaryButton);
            setupPrimaryButton(primaryButton);
        }
    }

    private void setupSecondaryButton(Button secondaryButton) {
        // Show Sign-Up button if login mode is Jetpack and signup from login is not enabled
        if (mLoginListener.getLoginMode() == LoginMode.JETPACK_STATS && !mIsSignupFromLoginEnabled) {
            secondaryButton.setText(formatUnderlinedText(R.string.login_email_button_signup));
            secondaryButton.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    mLoginListener.doStartSignup();
                    mGoogleApiClient.stopAutoManage(getActivity());

                    if (mGoogleApiClient.isConnected()) {
                        mGoogleApiClient.disconnect();
                    }
                }
            });
        } else if (mLoginListener.getLoginMode() == LoginMode.WOO_LOGIN_MODE) {
            secondaryButton.setText(getResources().getString(R.string.login_need_help_finding_connected_email));
            secondaryButton.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    mLoginListener.showHelpFindingConnectedEmail();
                }
            });
        } else {
            secondaryButton.setVisibility(View.GONE);
        }
    }

    private void setupPrimaryButton(Button primaryButton) {
        primaryButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                onContinueClicked();
            }
        });
    }

    private Spanned formatTosText(int stringResId) {
        final int primaryColorResId = ContextExtensionsKt.getColorResIdFromAttribute(getContext(), R.attr.colorPrimary);
        final String primaryColorHtml = HtmlUtils.colorResToHtmlColor(getContext(), primaryColorResId);
        return Html.fromHtml(getString(stringResId, "<u><font color='" + primaryColorHtml + "'>", "</font></u>"));
    }

    private Spanned formatUnderlinedText(int stringResId) {
        return Html.fromHtml(getString(stringResId, "<u>", "</u>"));
    }

    private void onContinueClicked() {
        next(getCleanedEmail());
    }

    private void onGoogleSigninClicked() {
        mAnalyticsListener.trackSocialButtonClick();
        ActivityUtils.hideKeyboardForced(mEmailInput.getEditText());

        if (NetworkUtils.checkConnection(getActivity())) {
            if (isAdded()) {
                mOldSitesIDs = SiteUtils.getCurrentSiteIds(mSiteStore, false);
                mIsSocialLogin = true;
                mLoginListener.addGoogleLoginFragment();
            } else {
                AppLog.e(T.NUX, "Google login could not be started.  LoginEmailFragment was not attached.");
                showErrorDialog(getString(R.string.login_error_generic_start));
            }
        }
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

        Bundle args = getArguments();
        if (args != null) {
            mLoginSiteUrl = args.getString(ARG_LOGIN_SITE_URL, "");
            mIsSignupFromLoginEnabled = args.getBoolean(ARG_SIGNUP_FROM_LOGIN_ENABLED, false);
            mIsSiteLoginEnabled = args.getBoolean(ARG_SITE_LOGIN_ENABLED, true);
            mShouldUseNewLayout = args.getBoolean(ARG_SHOULD_USE_NEW_LAYOUT, false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(LoginEmailFragment.this)
                .enableAutoManage(getActivity(), GOOGLE_API_CLIENT_ID, LoginEmailFragment.this)
                .addApi(Auth.CREDENTIALS_API)
                .build();
        showEmailError();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.stopAutoManage(getActivity());
            if (mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
        }
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
            if (savedInstanceState.containsKey(KEY_EMAIL_ERROR_RES)) {
                mCurrentEmailErrorRes = savedInstanceState.getInt(KEY_EMAIL_ERROR_RES);
            }
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
        if (mCurrentEmailErrorRes != null) {
            outState.putInt(KEY_EMAIL_ERROR_RES, mCurrentEmailErrorRes);
        }
    }

    @Override
    protected void buildToolbar(Toolbar toolbar, ActionBar actionBar) {
        if (mShouldUseNewLayout) {
            actionBar.setTitle(R.string.get_started);
        } else {
            super.buildToolbar(toolbar, actionBar);
        }
    }

    protected void next(String email) {
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        if (isValidEmail(email)) {
            clearEmailError();
            startProgress();
            mRequestedEmail = email;
            mDispatcher.dispatch(AccountActionBuilder.newIsAvailableEmailAction(email));
        } else {
            showEmailError(R.string.email_invalid);
        }
    }

    /**
     * This is cleared every time the text is changed or the email is valid so that if the user rotates the device, they
     * don't receive an unnecessary warning from a previous error.
     */
    private void clearEmailError() {
        mCurrentEmailErrorRes = null;
    }

    private void showEmailError() {
        if (mCurrentEmailErrorRes != null) {
            showEmailError(mCurrentEmailErrorRes);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginListener = null;
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
        clearEmailError();
    }

    private void showEmailError(int messageId) {
        mCurrentEmailErrorRes = messageId;
        String errorMessage = getString(messageId);
        mAnalyticsListener.trackFailure(errorMessage);
        mEmailInput.setError(errorMessage);
    }

    private void showErrorDialog(String message) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(getActivity())
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
            // we validate the email prior to making the request, but just to be safe...
            if (event.error.type == AccountStore.IsAvailableErrorType.INVALID) {
                showEmailError(R.string.email_invalid);
            } else {
                showErrorDialog(getString(R.string.error_generic_network));
            }
            return;
        }

        switch (event.type) {
            case EMAIL:
                if (event.isAvailable) {
                    ActivityUtils.hideKeyboardForced(mEmailInput);
                    if (mIsSignupFromLoginEnabled) {
                        if (mLoginListener != null) {
                            mLoginListener.gotUnregisteredEmail(event.value);
                        }
                    } else {
                        // email address is available on wpcom, so apparently the user can't login with that one.
                        mAnalyticsListener.trackFailure("Email not registered WP.com");
                        showEmailError(R.string.email_not_registered_wpcom);
                    }
                } else if (mLoginListener != null) {
                    ActivityUtils.hideKeyboardForced(mEmailInput);
                    mLoginListener.gotWpcomEmail(event.value, false);
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
        mAnalyticsListener.trackAnalyticsSignIn(true);
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
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        if (getContext() == null
            || googleApiAvailability.isGooglePlayServicesAvailable(getContext()) != ConnectionResult.SUCCESS) {
            AppLog.w(T.NUX, LOG_TAG + ": Couldn't start hint picker - Play Services unavailable");
            return;
        }
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
        } catch (ActivityNotFoundException exception) {
            AppLog.d(T.NUX, LOG_TAG + "Could not find any activity to handle email hint picker" + exception);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EMAIL_CREDENTIALS_REQUEST_CODE) {
            if (mEmailInput == null) {
                // Activity result received before the fragments onCreateView(), disregard result.
                return;
            }

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
