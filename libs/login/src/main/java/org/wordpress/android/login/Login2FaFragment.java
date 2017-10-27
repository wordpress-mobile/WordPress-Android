package org.wordpress.android.login;

import android.content.ClipboardManager;
import android.content.Context;
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
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.login.util.SiteUtils;
import org.wordpress.android.login.widgets.WPLoginInputRow;
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dagger.android.support.AndroidSupportInjection;

import static android.content.Context.CLIPBOARD_SERVICE;

public class Login2FaFragment extends LoginBaseFormFragment<LoginListener> implements TextWatcher,
        OnEditorCommitListener {
    private static final String KEY_IN_PROGRESS_MESSAGE_ID = "KEY_IN_PROGRESS_MESSAGE_ID";
    private static final String KEY_OLD_SITES_IDS = "KEY_OLD_SITES_IDS";

    private static final String ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS";
    private static final String ARG_PASSWORD = "ARG_PASSWORD";

    public static final String TAG = "login_2fa_fragment_tag";

    private static final Pattern TWO_STEP_AUTH_CODE = Pattern.compile("^[0-9]{6}");

    private WPLoginInputRow m2FaInput;

    private @StringRes int mInProgressMessageId;
    ArrayList<Integer> mOldSitesIDs;

    private String mEmailAddress;
    private String mPassword;

    public static Login2FaFragment newInstance(String emailAddress, String password) {
        Login2FaFragment fragment = new Login2FaFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL_ADDRESS, emailAddress);
        args.putString(ARG_PASSWORD, password);
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
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEmailAddress = getArguments().getString(ARG_EMAIL_ADDRESS);
        mPassword = getArguments().getString(ARG_PASSWORD);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        // retrieve mInProgressMessageId before super.onActivityCreated() so the string will be available to the
        //  progress bar helper if in progress
        if (savedInstanceState != null) {
            mInProgressMessageId = savedInstanceState.getInt(KEY_IN_PROGRESS_MESSAGE_ID, 0);
            mOldSitesIDs = savedInstanceState.getIntegerArrayList(KEY_OLD_SITES_IDS);
        } else {
            mLoginListener.track(AnalyticsTracker.Stat.LOGIN_TWO_FACTOR_FORM_VIEWED);
        }

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_IN_PROGRESS_MESSAGE_ID, mInProgressMessageId);
        outState.putIntegerArrayList(KEY_OLD_SITES_IDS, mOldSitesIDs);
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

        AccountStore.AuthenticatePayload payload = new AccountStore.AuthenticatePayload(mEmailAddress, mPassword);
        payload.twoStepCode = twoStepCode;
        payload.shouldSendTwoStepSms = shouldSendTwoStepSMS;
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
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
    }

    private void show2FaError(String message) {
        m2FaInput.setError(message);
    }

    @Override
    protected void endProgress() {
        super.endProgress();
        mInProgressMessageId = 0;
    }

    private void handleAuthError(AuthenticationErrorType error, String errorMessage) {
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
            mLoginListener.track(AnalyticsTracker.Stat.LOGIN_FAILED, event.getClass().getSimpleName(),
                    event.error.type.toString(), event.error.message);

            if (isAdded()) {
                handleAuthError(event.error.type, event.error.message);
            }

            return;
        }

        AppLog.i(T.NUX, "onAuthenticationChanged: " + event.toString());

        doFinishLogin();
    }

    @Override
    protected void onLoginFinished() {
        mLoginListener.trackAnalyticsSignIn(mAccountStore, mSiteStore, true);

        mLoginListener.loggedInViaPassword(mOldSitesIDs);
    }
}
