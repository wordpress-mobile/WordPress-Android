package org.wordpress.android.login;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.fido.Fido;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateTwoFactorPayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.fluxc.store.AccountStore.FinishWebauthnChallengePayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnSocialChanged;
import org.wordpress.android.fluxc.store.AccountStore.PushSocialAuthPayload;
import org.wordpress.android.fluxc.store.AccountStore.PushSocialPayload;
import org.wordpress.android.fluxc.store.AccountStore.PushSocialSmsPayload;
import org.wordpress.android.fluxc.store.AccountStore.StartWebauthnChallengePayload;
import org.wordpress.android.fluxc.store.AccountStore.WebauthnChallengeReceived;
import org.wordpress.android.fluxc.store.AccountStore.WebauthnPasskeyAuthenticated;
import org.wordpress.android.login.util.SiteUtils;
import org.wordpress.android.login.webauthn.PasskeyRequest;
import org.wordpress.android.login.webauthn.Fido2ClientHandler;
import org.wordpress.android.login.widgets.WPLoginInputRow;
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.CLIPBOARD_SERVICE;

import dagger.android.support.AndroidSupportInjection;

public class Login2FaFragment extends LoginBaseFormFragment<LoginListener> implements TextWatcher,
        OnEditorCommitListener {
    private static final String KEY_2FA_TYPE = "KEY_2FA_TYPE";
    private static final String KEY_IN_PROGRESS_MESSAGE_ID = "KEY_IN_PROGRESS_MESSAGE_ID";
    private static final String KEY_NONCE_AUTHENTICATOR = "KEY_NONCE_AUTHENTICATOR";
    private static final String KEY_NONCE_BACKUP = "KEY_NONCE_BACKUP";
    private static final String KEY_NONCE_SMS = "KEY_NONCE_SMS";
    private static final String KEY_OLD_SITES_IDS = "KEY_OLD_SITES_IDS";
    private static final String KEY_SMS_NUMBER = "KEY_SMS_NUMBER";
    private static final String KEY_SMS_SENT = "KEY_SMS_SENT";

    private static final String ARG_2FA_ID_TOKEN = "ARG_2FA_ID_TOKEN";
    private static final String ARG_2FA_IS_SOCIAL = "ARG_2FA_IS_SOCIAL";
    private static final String ARG_2FA_IS_SOCIAL_CONNECT = "ARG_2FA_IS_SOCIAL_CONNECT";
    private static final String ARG_2FA_NONCE_AUTHENTICATOR = "ARG_2FA_NONCE_AUTHENTICATOR";
    private static final String ARG_2FA_NONCE_BACKUP = "ARG_2FA_NONCE_BACKUP";
    private static final String ARG_2FA_NONCE_SMS = "ARG_2FA_NONCE_SMS";
    private static final String ARG_2FA_SOCIAL_SERVICE = "ARG_2FA_SOCIAL_SERVICE";
    private static final String ARG_2FA_USER_ID = "ARG_2FA_USER_ID";
    private static final String ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS";
    private static final String ARG_PASSWORD = "ARG_PASSWORD";
    private static final String ARG_WEBAUTHN_NONCE = "WEBAUTHN_NONCE";
    private static final String ARG_2FA_SUPPORTED_AUTH_TYPES = "ARG_2FA_SUPPORTED_AUTH_TYPES";
    private static final int LENGTH_NONCE_AUTHENTICATOR = 6;
    private static final int LENGTH_NONCE_BACKUP = 8;
    private static final int LENGTH_NONCE_SMS = 7;

    private static final String TWO_FACTOR_TYPE_AUTHENTICATOR = "authenticator";
    private static final String TWO_FACTOR_TYPE_BACKUP = "backup";

    public static final String TWO_FACTOR_TYPE_SMS = "sms";
    public static final String TAG = "login_2fa_fragment_tag";

    private static final Pattern TWO_STEP_AUTH_CODE = Pattern.compile("^[0-9]{6}");

    private WPLoginInputRow m2FaInput;

    private static final @StringRes int DEFAULT_PROGRESS_MESSAGE_ID = R.string.logging_in;
    private @StringRes int mInProgressMessageId = DEFAULT_PROGRESS_MESSAGE_ID;

    ArrayList<Integer> mOldSitesIDs;

    private Button mOtpButton;
    private Button mSecurityKeyButton;
    private String mEmailAddress;
    private String mIdToken;
    private String mNonce;
    private String mNonceAuthenticator;
    private String mWebauthnNonce;
    private String mNonceBackup;
    private String mNonceSms;
    private String mPassword;
    private String mPhoneNumber;
    private String mService;
    private String mType;
    private String mUserId;
    private TextView mLabel;
    private boolean mIsSocialLogin;
    private boolean mIsSocialLoginConnect;
    private boolean mSentSmsCode;
    private List<SupportedAuthTypes> mSupportedAuthTypes;
    @Nullable private Fido2ClientHandler mFido2ClientHandler = null;
    @Nullable private ActivityResultLauncher<IntentSenderRequest> mResultLauncher = null;

    public static Login2FaFragment newInstance(String emailAddress, String password) {
        Login2FaFragment fragment = new Login2FaFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL_ADDRESS, emailAddress);
        args.putString(ARG_PASSWORD, password);
        fragment.setArguments(args);
        return fragment;
    }

    public static Login2FaFragment newInstance(String emailAddress, String password,
                                               String userId, String webauthnNonce,
                                               String authenticatorNonce, String backupNonce,
                                               String smsNonce, List<String> authTypes) {
        boolean supportsWebauthn = webauthnNonce != null && !webauthnNonce.isEmpty();
        Login2FaFragment fragment = new Login2FaFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL_ADDRESS, emailAddress);
        args.putString(ARG_PASSWORD, password);
        args.putString(ARG_2FA_USER_ID, userId);
        args.putString(ARG_WEBAUTHN_NONCE, webauthnNonce);
        args.putString(ARG_2FA_NONCE_AUTHENTICATOR, authenticatorNonce);
        args.putString(ARG_2FA_NONCE_BACKUP, backupNonce);
        args.putString(ARG_2FA_NONCE_SMS, smsNonce);
        args.putStringArrayList(ARG_2FA_SUPPORTED_AUTH_TYPES, new ArrayList<>(authTypes));
        fragment.setArguments(args);
        return fragment;
    }

    public static Login2FaFragment newInstanceSocial(String emailAddress, String userId,
                                                     String nonceAuthenticator, String nonceBackup,
                                                     String nonceSms, String nonceWebauthn,
                                                     List<String> authTypes) {
        Login2FaFragment fragment = new Login2FaFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL_ADDRESS, emailAddress);
        args.putString(ARG_2FA_USER_ID, userId);
        args.putString(ARG_2FA_NONCE_AUTHENTICATOR, nonceAuthenticator);
        args.putString(ARG_2FA_NONCE_BACKUP, nonceBackup);
        args.putString(ARG_2FA_NONCE_SMS, nonceSms);
        args.putString(ARG_WEBAUTHN_NONCE, nonceWebauthn);
        args.putBoolean(ARG_2FA_IS_SOCIAL, true);
        // Social account connected, connect call not needed.
        args.putBoolean(ARG_2FA_IS_SOCIAL_CONNECT, false);
        args.putStringArrayList(ARG_2FA_SUPPORTED_AUTH_TYPES, new ArrayList<>(authTypes));
        fragment.setArguments(args);
        return fragment;
    }

    public static Login2FaFragment newInstanceSocialConnect(String emailAddress, String password,
                                                            String idToken, String service) {
        Login2FaFragment fragment = new Login2FaFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL_ADDRESS, emailAddress);
        args.putString(ARG_PASSWORD, password);
        args.putString(ARG_2FA_ID_TOKEN, idToken);
        args.putBoolean(ARG_2FA_IS_SOCIAL_CONNECT, true);
        args.putString(ARG_2FA_SOCIAL_SERVICE, service);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.login_2fa_screen;
    }

    @Override
    protected @LayoutRes int getProgressBarText() {
        return mInProgressMessageId;
    }

    @Override
    protected void setupLabel(@NonNull TextView label) {
        label.setText(mSentSmsCode ? getString(R.string.enter_verification_code_sms, mPhoneNumber)
                : getString(R.string.enter_verification_code));
        mLabel = label;
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        // important for accessibility - talkback
        getActivity().setTitle(R.string.verification_2fa_screen_title);
        m2FaInput = rootView.findViewById(R.id.login_2fa_row);
        m2FaInput.addTextChangedListener(this);
        m2FaInput.setOnEditorCommitListener(this);

        // restrict the allowed input chars to just numbers
        m2FaInput.getEditText().setKeyListener(DigitsKeyListener.getInstance("0123456789"));

        boolean isSmsEnabled = mSupportedAuthTypes.contains(SupportedAuthTypes.PUSH);
        mOtpButton = rootView.findViewById(R.id.login_otp_button);
        mOtpButton.setVisibility(isSmsEnabled ? View.VISIBLE : View.GONE);
        mOtpButton.setText(mSentSmsCode ? R.string.login_text_otp_another : R.string.login_text_otp);
        mOtpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAdded()) {
                    mAnalyticsListener.trackSendCodeWithTextClicked();
                    doAuthAction(R.string.requesting_otp, "", true);
                }
            }
        });

        boolean isSecurityKeyEnabled = mSupportedAuthTypes.contains(SupportedAuthTypes.WEBAUTHN);
        mSecurityKeyButton = rootView.findViewById(R.id.login_security_key_button);
        mSecurityKeyButton.setVisibility(isSecurityKeyEnabled ? View.VISIBLE : View.GONE);
        mSecurityKeyButton.setOnClickListener(view -> doAuthWithSecurityKeyAction());
    }

    @Override
    protected void setupBottomButton(Button button) {
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                next();
            }
        });
    }

    @Override
    protected void buildToolbar(Toolbar toolbar, ActionBar actionBar) {
        actionBar.setTitle(R.string.log_in);
    }

    @Override
    protected EditText getEditTextToFocusOnStart() {
        return m2FaInput.getEditText();
    }

    @Override
    protected void onHelp() {
        if (mLoginListener != null) {
            mLoginListener.help2FaScreen(mEmailAddress);
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

        mEmailAddress = getArguments().getString(ARG_EMAIL_ADDRESS);
        mPassword = getArguments().getString(ARG_PASSWORD);
        mNonceAuthenticator = getArguments().getString(ARG_2FA_NONCE_AUTHENTICATOR);
        mNonceBackup = getArguments().getString(ARG_2FA_NONCE_BACKUP);
        mNonceSms = getArguments().getString(ARG_2FA_NONCE_SMS);
        mUserId = getArguments().getString(ARG_2FA_USER_ID);
        mIdToken = getArguments().getString(ARG_2FA_ID_TOKEN);
        mIsSocialLogin = getArguments().getBoolean(ARG_2FA_IS_SOCIAL);
        mIsSocialLoginConnect = getArguments().getBoolean(ARG_2FA_IS_SOCIAL_CONNECT);
        mService = getArguments().getString(ARG_2FA_SOCIAL_SERVICE);
        mWebauthnNonce = getArguments().getString(ARG_WEBAUTHN_NONCE);
        mSupportedAuthTypes = handleSupportedAuthTypesParameter(
                getArguments().getStringArrayList(ARG_2FA_SUPPORTED_AUTH_TYPES));

        if (savedInstanceState != null) {
            // Overwrite argument nonce values with saved state values on device rotation.
            mNonceAuthenticator = savedInstanceState.getString(KEY_NONCE_AUTHENTICATOR);
            mNonceBackup = savedInstanceState.getString(KEY_NONCE_BACKUP);
            mNonceSms = savedInstanceState.getString(KEY_NONCE_SMS);
            // Restore set two-factor authentication type value on device rotation.
            mType = savedInstanceState.getString(KEY_2FA_TYPE);
            mPhoneNumber = savedInstanceState.getString(KEY_SMS_NUMBER);
            mSentSmsCode = savedInstanceState.getBoolean(KEY_SMS_SENT);
        }

        mResultLauncher =
                registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        onCredentialsResultAvailable(result.getData());
                    } else {
                        handleWebauthnError();
                    }
                });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        // retrieve mInProgressMessageId before super.onActivityCreated() so the string will be available to the
        //  progress bar helper if in progress
        if (savedInstanceState != null) {
            mInProgressMessageId = savedInstanceState.getInt(KEY_IN_PROGRESS_MESSAGE_ID, DEFAULT_PROGRESS_MESSAGE_ID);
            mOldSitesIDs = savedInstanceState.getIntegerArrayList(KEY_OLD_SITES_IDS);
        } else {
            mAnalyticsListener.trackTwoFactorFormViewed();
        }
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_IN_PROGRESS_MESSAGE_ID, mInProgressMessageId);
        outState.putIntegerArrayList(KEY_OLD_SITES_IDS, mOldSitesIDs);
        outState.putString(KEY_NONCE_AUTHENTICATOR, mNonceAuthenticator);
        outState.putString(KEY_NONCE_BACKUP, mNonceBackup);
        outState.putString(KEY_NONCE_SMS, mNonceSms);
        outState.putString(KEY_2FA_TYPE, mType);
        outState.putString(KEY_SMS_NUMBER, mPhoneNumber);
        outState.putBoolean(KEY_SMS_SENT, mSentSmsCode);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Insert authentication code if copied to clipboard
        if (TextUtils.isEmpty(m2FaInput.getEditText().getText())) {
            m2FaInput.setText(getAuthCodeFromClipboard());
        }

        updateContinueButtonEnabledStatus();
    }

    protected void next() {
        mAnalyticsListener.trackSubmit2faCodeClicked();
        if (TextUtils.isEmpty(m2FaInput.getEditText().getText())) {
            show2FaError(getString(R.string.login_empty_2fa));
            return;
        }

        doAuthAction(R.string.logging_in, m2FaInput.getEditText().getText().toString(), false);
    }

    private void doAuthAction(@StringRes int messageId, String twoStepCode, boolean shouldSendTwoStepSMS) {
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        mInProgressMessageId = messageId;
        startProgress();

        mOldSitesIDs = SiteUtils.getCurrentSiteIds(mSiteStore, false);

        if (mIsSocialLogin) {
            if (shouldSendTwoStepSMS) {
                PushSocialSmsPayload payload = new PushSocialSmsPayload(mUserId, mNonceSms);
                mDispatcher.dispatch(AccountActionBuilder.newPushSocialSmsAction(payload));
            } else {
                setAuthCodeTypeAndNonce(twoStepCode);
                PushSocialAuthPayload payload = new PushSocialAuthPayload(mUserId, mType, mNonce,
                        twoStepCode);
                mDispatcher.dispatch(AccountActionBuilder.newPushSocialAuthAction(payload));
            }
        } else {
            AuthenticateTwoFactorPayload payload = new AuthenticateTwoFactorPayload(mEmailAddress,
                    mPassword, twoStepCode, shouldSendTwoStepSMS);
            mDispatcher.dispatch(AuthenticationActionBuilder
                    .newAuthenticateTwoFactorAction(payload));
        }
    }

    private String getAuthCodeFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);

        if (clipboard.getPrimaryClip() != null && clipboard.getPrimaryClip().getItemAt(0) != null
            && clipboard.getPrimaryClip().getItemAt(0).getText() != null) {
            String code = clipboard.getPrimaryClip().getItemAt(0).getText().toString();

            final Matcher twoStepAuthCodeMatcher = TWO_STEP_AUTH_CODE.matcher("");
            twoStepAuthCodeMatcher.reset(code);

            if (!code.isEmpty() && twoStepAuthCodeMatcher.matches()) {
                return code;
            }
        }

        return "";
    }

    private void setAuthCodeTypeAndNonce(String twoStepCode) {
        switch (twoStepCode.length()) {
            case LENGTH_NONCE_AUTHENTICATOR:
                mType = TWO_FACTOR_TYPE_AUTHENTICATOR;
                mNonce = mNonceAuthenticator;
                break;
            case LENGTH_NONCE_BACKUP:
                mType = TWO_FACTOR_TYPE_BACKUP;
                mNonce = mNonceBackup;
                break;
            case LENGTH_NONCE_SMS:
                mType = TWO_FACTOR_TYPE_SMS;
                mNonce = mNonceSms;
                break;
        }
    }

    @Override
    public void onEditorCommit() {
        next();
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        show2FaError(null);
        updateContinueButtonEnabledStatus();
    }

    private void show2FaError(@Nullable String message) {
        if (!TextUtils.isEmpty(message)) {
            mAnalyticsListener.trackFailure(message);
        }
        m2FaInput.setError(message);
    }

    private void updateContinueButtonEnabledStatus() {
        String currentVerificationCode = m2FaInput.getEditText().getText().toString();
        getBottomButton().setEnabled(!currentVerificationCode.trim().isEmpty());
    }

    @Override
    protected void endProgress() {
        super.endProgress();
        mInProgressMessageId = DEFAULT_PROGRESS_MESSAGE_ID;
    }

    private void handleAuthError(AuthenticationErrorType error, String errorMessage) {
        switch (error) {
            case INVALID_OTP:
                show2FaError(getString(R.string.invalid_verification_code));
                break;
            case NEEDS_2FA:
                // we get this error when requesting a verification code sent via SMS so, just ignore it.
                break;
            case INVALID_REQUEST:
                // TODO: FluxC: could be specific?
            case WEBAUTHN_FAILED:
                mAnalyticsListener.trackLoginSecurityKeyFailure();
                ToastUtils.showToast(getActivity(),
                        errorMessage == null ? getString(R.string.error_generic) : errorMessage);
                break;
            default:
                AppLog.e(T.NUX, "Server response: " + errorMessage);
                mAnalyticsListener.trackFailure(errorMessage);
                ToastUtils.showToast(getActivity(),
                        errorMessage == null ? getString(R.string.error_generic) : errorMessage);
                break;
        }
    }

    private void showErrorDialog(String message) {
        mAnalyticsListener.trackFailure(message);
        AlertDialog dialog = new MaterialAlertDialogBuilder(getActivity())
                .setMessage(message)
                .setPositiveButton(R.string.login_error_button, null)
                .create();
        dialog.show();
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            endProgress();

            AppLog.e(T.API, "onAuthenticationChanged has error: " + event.error.type + " - " + event.error.message);
            mAnalyticsListener.trackLoginFailed(event.getClass().getSimpleName(),
                    event.error.type.toString(), event.error.message);

            if (mIsSocialLogin) {
                mAnalyticsListener.trackSocialFailure(event.getClass().getSimpleName(),
                        event.error.type.toString(), event.error.message);
            }

            if (isAdded()) {
                handleAuthError(event.error.type, event.error.message);
            }

            return;
        }

        AppLog.i(T.NUX, "onAuthenticationChanged: " + event.toString());

        if (mIsSocialLoginConnect) {
            PushSocialPayload payload = new PushSocialPayload(mIdToken, mService);
            mDispatcher.dispatch(AccountActionBuilder.newPushSocialConnectAction(payload));
        } else {
            doFinishLogin();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocialChanged(OnSocialChanged event) {
        if (event.isError()) {
            switch (event.error.type) {
                // Two-factor authentication code was incorrect; save new nonce for another try.
                case INVALID_TWO_STEP_CODE:
                    endProgress();

                    switch (mType) {
                        case TWO_FACTOR_TYPE_AUTHENTICATOR:
                            mNonceAuthenticator = event.error.nonce;
                            break;
                        case TWO_FACTOR_TYPE_BACKUP:
                            mNonceBackup = event.error.nonce;
                            break;
                        case TWO_FACTOR_TYPE_SMS:
                            mNonceSms = event.error.nonce;
                            break;
                    }

                    show2FaError(getString(R.string.invalid_verification_code));
                    break;
                // Two-factor authentication via SMS failed; show message, log error,
                // and replace SMS nonce with response.
                case INVALID_TWO_STEP_NONCE:
                case NO_PHONE_NUMBER_FOR_ACCOUNT:
                case SMS_AUTHENTICATION_UNAVAILABLE:
                case SMS_CODE_THROTTLED:
                    endProgress();
                    showErrorDialog(event.error.message);
                    AppLog.e(T.API, event.error.type + ": " + event.error.message);
                    mNonceSms = event.error.nonce;
                    break;
                case UNABLE_CONNECT:
                    AppLog.e(T.API, "Unable to connect WordPress.com account to social account.");
                    break;
                case USER_ALREADY_ASSOCIATED:
                    AppLog.e(T.API, "This social account is already associated with a WordPress.com account.");
                    break;
            }

            // Finish login on social connect error.
            if (mIsSocialLoginConnect) {
                mAnalyticsListener.trackSocialConnectFailure();
                doFinishLogin();
            }
            // Two-factor authentication code was sent via SMS to account phone number; replace SMS nonce with response.
        } else if (!TextUtils.isEmpty(event.phoneNumber) && !TextUtils.isEmpty(event.nonce)) {
            endProgress();
            mPhoneNumber = event.phoneNumber;
            mNonceSms = event.nonce;
            setTextForSms();
        } else {
            if (mIsSocialLoginConnect) {
                mAnalyticsListener.trackSocialConnectSuccess();
            }
            doFinishLogin();
        }
    }

    @Override
    protected void onLoginFinished() {
        mAnalyticsListener.trackAnalyticsSignIn(true);

        mLoginListener.startPostLoginServices();

        if (mIsSocialLogin) {
            mLoginListener.loggedInViaSocialAccount(mOldSitesIDs, false);
        } else {
            mLoginListener.loggedInViaPassword(mOldSitesIDs);
        }
    }

    private void setTextForSms() {
        mLabel.setText(getString(R.string.enter_verification_code_sms, mPhoneNumber));
        mOtpButton.setText(getString(R.string.login_text_otp_another));
        mSentSmsCode = true;
    }

    private void doAuthWithSecurityKeyAction() {
        mAnalyticsListener.trackUseSecurityKeyClicked();
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        startProgress();
        mOldSitesIDs = SiteUtils.getCurrentSiteIds(mSiteStore, false);

        StartWebauthnChallengePayload payload = new StartWebauthnChallengePayload(
                mUserId, mWebauthnNonce);
        mDispatcher.dispatch(AuthenticationActionBuilder
                .newStartSecurityKeyChallengeAction(payload));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWebauthnChallengeReceived(WebauthnChallengeReceived event) {
        if (event.isError()) {
            endProgress();
            handleAuthError(event.error.type, getString(R.string.login_error_security_key));
            return;
        }

        new PasskeyRequest(
                requireContext(),
                event.mUserId,
                event.mChallengeInfo.getTwoStepNonce(),
                event.mRawChallengeInfoJson,
                result -> {
                    mDispatcher.dispatch(
                            AuthenticationActionBuilder.newFinishSecurityKeyChallengeAction(
                                    result));
                    return null;
                },
                error -> {
                    handleWebauthnError();
                    return null;
                }
        );
    }

    private void onCredentialsResultAvailable(@NonNull Intent resultData) {
        if (resultData.hasExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)) {
            byte[] credentialBytes = resultData.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA);
            if (credentialBytes == null || mFido2ClientHandler == null) {
                handleWebauthnError();
                return;
            }

            PublicKeyCredential credentials =
                    PublicKeyCredential.deserializeFromBytes(credentialBytes);
            FinishWebauthnChallengePayload payload =
                    mFido2ClientHandler.onCredentialsAvailable(credentials);
            mDispatcher.dispatch(
                    AuthenticationActionBuilder.newFinishSecurityKeyChallengeAction(payload));
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSecurityKeyCheckFinished(WebauthnPasskeyAuthenticated event) {
        if (event.isError()) {
            endProgress();
            handleAuthError(event.error.type, getString(R.string.login_error_security_key));
            return;
        }
        mAnalyticsListener.trackLoginSecurityKeySuccess();
        doFinishLogin();
    }

    private void handleWebauthnError() {
        String errorMessage = getString(R.string.login_error_security_key);
        endProgress();
        handleAuthError(AuthenticationErrorType.WEBAUTHN_FAILED, errorMessage);
    }

    @NonNull private ArrayList<SupportedAuthTypes> handleSupportedAuthTypesParameter(
            ArrayList<String> supportedTypes) {
        ArrayList<SupportedAuthTypes> supportedAuthTypes = new ArrayList<>();
        if (supportedTypes != null) {
            for (String type : supportedTypes) {
                SupportedAuthTypes parsedType = SupportedAuthTypes.fromString(type);
                if (parsedType != SupportedAuthTypes.UNKNOWN) {
                    supportedAuthTypes.add(parsedType);
                }
            }
        }
        return supportedAuthTypes;
    }

    public enum SupportedAuthTypes {
        WEBAUTHN,
        BACKUP,
        AUTHENTICATOR,
        PUSH,
        UNKNOWN;

        static SupportedAuthTypes fromString(String value) {
            switch (value) {
                case "webauthn":
                    return WEBAUTHN;
                case "backup":
                    return BACKUP;
                case "authenticator":
                    return AUTHENTICATOR;
                case "push":
                    return PUSH;
                default:
                    return UNKNOWN;
            }
        }
    }
}
