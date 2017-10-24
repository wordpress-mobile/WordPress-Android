package org.wordpress.android.ui.accounts.login;

import android.content.ClipboardManager;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
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

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnSocialChanged;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPLoginInputRow;
import org.wordpress.android.widgets.WPLoginInputRow.OnEditorCommitListener;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.CLIPBOARD_SERVICE;

public class Login2FaFragment extends LoginBaseFormFragment<LoginListener> implements TextWatcher, OnEditorCommitListener {
    private static final String KEY_2FA_TYPE = "KEY_2FA_TYPE";
    private static final String KEY_IN_PROGRESS_MESSAGE_ID = "KEY_IN_PROGRESS_MESSAGE_ID";
    private static final String KEY_NONCE_AUTHENTICATOR = "KEY_NONCE_AUTHENTICATOR";
    private static final String KEY_NONCE_BACKUP = "KEY_NONCE_BACKUP";
    private static final String KEY_NONCE_SMS = "KEY_NONCE_SMS";
    private static final String KEY_OLD_SITES_IDS = "KEY_OLD_SITES_IDS";

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
    private static final int LENGTH_NONCE_AUTHENTICATOR = 6;
    private static final int LENGTH_NONCE_BACKUP = 8;
    private static final int LENGTH_NONCE_SMS = 7;

    private static final String TWO_FACTOR_TYPE_AUTHENTICATOR = "authenticator";
    private static final String TWO_FACTOR_TYPE_BACKUP = "backup";
    private static final String TWO_FACTOR_TYPE_SMS = "sms";

    public static final String TAG = "login_2fa_fragment_tag";

    private WPLoginInputRow m2FaInput;

    private @StringRes int mInProgressMessageId;
    ArrayList<Integer> mOldSitesIDs;

    private String mEmailAddress;
    private String mIdToken;
    private String mNonce;
    private String mNonceAuthenticator;
    private String mNonceBackup;
    private String mNonceSms;
    private String mPassword;
    private String mService;
    private String mType;
    private String mUserId;
    private boolean isSocialLogin;
    private boolean isSocialLoginConnect;

    public static Login2FaFragment newInstance(String emailAddress, String password) {
        Login2FaFragment fragment = new Login2FaFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL_ADDRESS, emailAddress);
        args.putString(ARG_PASSWORD, password);
        fragment.setArguments(args);
        return fragment;
    }

    public static Login2FaFragment newInstanceSocial(String emailAddress, String userId,
                                                     String nonceAuthenticator, String nonceBackup,
                                                     String nonceSms) {
        Login2FaFragment fragment = new Login2FaFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL_ADDRESS, emailAddress);
        args.putString(ARG_2FA_USER_ID, userId);
        args.putString(ARG_2FA_NONCE_AUTHENTICATOR, nonceAuthenticator);
        args.putString(ARG_2FA_NONCE_BACKUP, nonceBackup);
        args.putString(ARG_2FA_NONCE_SMS, nonceSms);
        args.putBoolean(ARG_2FA_IS_SOCIAL, true);
        // Social account connected, connect call not needed.
        args.putBoolean(ARG_2FA_IS_SOCIAL_CONNECT, false);
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
    protected void setupLabel(TextView label) {
        // nothing special to do, just leave the string setup via the xml layout file
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        m2FaInput = (WPLoginInputRow) rootView.findViewById(R.id.login_2fa_row);
        m2FaInput.addTextChangedListener(this);
        m2FaInput.setOnEditorCommitListener(this);

        // restrict the allowed input chars to just numbers
        m2FaInput.getEditText().setKeyListener(DigitsKeyListener.getInstance("0123456789"));
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        secondaryButton.setText(R.string.login_text_otp);
        secondaryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAdded()) {
                    doAuthAction(R.string.requesting_otp, "", true);
                }
            }
        });

        primaryButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                next();
            }
        });
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        mEmailAddress = getArguments().getString(ARG_EMAIL_ADDRESS);
        mPassword = getArguments().getString(ARG_PASSWORD);
        mNonceAuthenticator = getArguments().getString(ARG_2FA_NONCE_AUTHENTICATOR);
        mNonceBackup = getArguments().getString(ARG_2FA_NONCE_BACKUP);
        mNonceSms = getArguments().getString(ARG_2FA_NONCE_SMS);
        mUserId = getArguments().getString(ARG_2FA_USER_ID);
        mIdToken = getArguments().getString(ARG_2FA_ID_TOKEN);
        isSocialLogin = getArguments().getBoolean(ARG_2FA_IS_SOCIAL);
        isSocialLoginConnect = getArguments().getBoolean(ARG_2FA_IS_SOCIAL_CONNECT);
        mService = getArguments().getString(ARG_2FA_SOCIAL_SERVICE);

        if (savedInstanceState != null) {
            // Overwrite argument nonce values with saved state values on device rotation.
            mNonceAuthenticator = savedInstanceState.getString(KEY_NONCE_AUTHENTICATOR);
            mNonceBackup = savedInstanceState.getString(KEY_NONCE_BACKUP);
            mNonceSms = savedInstanceState.getString(KEY_NONCE_SMS);
            // Restore set two-factor authentication type value on device rotation.
            mType = savedInstanceState.getString(KEY_2FA_TYPE);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        // retrieve mInProgressMessageId before super.onActivityCreated() so the string will be available to the
        //  progress bar helper if in progress
        if (savedInstanceState != null) {
            mInProgressMessageId = savedInstanceState.getInt(KEY_IN_PROGRESS_MESSAGE_ID, 0);
            mOldSitesIDs = savedInstanceState.getIntegerArrayList(KEY_OLD_SITES_IDS);
        } else {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_TWO_FACTOR_FORM_VIEWED);
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
    }

    @Override
    public void onResume() {
        super.onResume();

        // Insert authentication code if copied to clipboard
        if (TextUtils.isEmpty(m2FaInput.getEditText().getText())) {
            m2FaInput.setText(getAuthCodeFromClipboard());
        }
    }

    protected void next() {
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

        if (isSocialLogin && !shouldSendTwoStepSMS) {
            setAuthCodeTypeAndNonce(twoStepCode);
            AccountStore.PushSocialAuthPayload payload = new AccountStore.PushSocialAuthPayload(mUserId, mType, mNonce,
                    twoStepCode);
            mDispatcher.dispatch(AccountActionBuilder.newPushSocialAuthAction(payload));
        } else {
            AccountStore.AuthenticatePayload payload = new AccountStore.AuthenticatePayload(mEmailAddress, mPassword);
            payload.twoStepCode = twoStepCode;
            payload.shouldSendTwoStepSms = shouldSendTwoStepSMS;
            mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        }
    }

    private String getAuthCodeFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);

        if (clipboard.getPrimaryClip() != null && clipboard.getPrimaryClip().getItemAt(0) != null
                && clipboard.getPrimaryClip().getItemAt(0).getText() != null) {
            String code = clipboard.getPrimaryClip().getItemAt(0).getText().toString();

            final Pattern TWO_STEP_AUTH_CODE = Pattern.compile("^[0-9]{6}");
            final Matcher twoStepAuthCodeMatcher = TWO_STEP_AUTH_CODE.matcher("");
            twoStepAuthCodeMatcher.reset(code);

            if (!code.isEmpty() && twoStepAuthCodeMatcher.matches()) {
                return code;
            }
        }

        return "";
    }

    private void setAuthCodeTypeAndNonce(String twoStepCode) {
        switch(twoStepCode.length()) {
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
    public void OnEditorCommit() {
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
    }

    private void show2FaError(String message) {
        m2FaInput.setError(message);
    }

    @Override
    protected void endProgress() {
        super.endProgress();
        mInProgressMessageId = 0;
    }

    private void handleAuthError(AccountStore.AuthenticationErrorType error, String errorMessage) {
        switch (error) {
            case INVALID_OTP:
                show2FaError(errorMessage);
                break;
            case NEEDS_2FA:
                // we get this error when requesting a verification code sent via SMS so, just ignore it.
                break;
            case INVALID_REQUEST:
                // TODO: FluxC: could be specific?
            default:
                AppLog.e(T.NUX, "Server response: " + errorMessage);

                ToastUtils.showToast(getActivity(),
                        errorMessage == null ? getString(R.string.error_generic) : errorMessage);
                break;
        }
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            endProgress();

            AppLog.e(T.API, "onAuthenticationChanged has error: " + event.error.type + " - " + event.error.message);
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FAILED, event.getClass().getSimpleName(),
                    event.error.type.toString(), event.error.message);

            if (isAdded()) {
                handleAuthError(event.error.type, event.error.message);
            }

            return;
        }

        AppLog.i(T.NUX, "onAuthenticationChanged: " + event.toString());

        if (isSocialLoginConnect) {
            AccountStore.PushSocialLoginPayload payload = new AccountStore.PushSocialLoginPayload(mIdToken, mService);
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
                case UNABLE_CONNECT:
                    AppLog.e(T.API, "Unable to connect WordPress.com account to social account.");
                    break;
                case USER_ALREADY_ASSOCIATED:
                    AppLog.e(T.API, "This social account is already associated with a WordPress.com account.");
                    break;
            }

            // Finish login on social connect error.
            if (isSocialLoginConnect) {
                doFinishLogin();
            }
        } else {
            doFinishLogin();
        }
    }

    @Override
    protected void onLoginFinished() {
        AnalyticsUtils.trackAnalyticsSignIn(mAccountStore, mSiteStore, true);

        if (isSocialLogin) {
            mLoginListener.loggedInViaSocialAccount(mOldSitesIDs);
        } else {
            mLoginListener.loggedInViaPassword(mOldSitesIDs);
        }
    }
}
