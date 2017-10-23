package org.wordpress.android.ui.accounts.login;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
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

public class LoginEmailPasswordFragment extends LoginBaseFormFragment<LoginListener> implements TextWatcher,
        OnEditorCommitListener {
    private static final String KEY_REQUESTED_PASSWORD = "KEY_REQUESTED_PASSWORD";
    private static final String KEY_OLD_SITES_IDS = "KEY_OLD_SITES_IDS";

    private static final String ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS";
    private static final String ARG_PASSWORD = "ARG_PASSWORD";
    private static final String ARG_SOCIAL_ID_TOKEN = "ARG_SOCIAL_ID_TOKEN";
    private static final String ARG_SOCIAL_LOGIN = "ARG_SOCIAL_LOGIN";
    private static final String ARG_SOCIAL_SERVICE = "ARG_SOCIAL_SERVICE";

    private static final String FORGOT_PASSWORD_URL_WPCOM = "https://wordpress.com/";

    public static final String TAG = "login_email_password_fragment_tag";

    private WPLoginInputRow mPasswordInput;

    private String mRequestedPassword;
    ArrayList<Integer> mOldSitesIDs;

    private String mEmailAddress;
    private String mIdToken;
    private String mPassword;
    private String mService;
    private boolean isSocialLogin;

    public static LoginEmailPasswordFragment newInstance(String emailAddress, String password,
                                                         String idToken, String service,
                                                         boolean isSocialLogin) {
        LoginEmailPasswordFragment fragment = new LoginEmailPasswordFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL_ADDRESS, emailAddress);
        args.putString(ARG_PASSWORD, password);
        args.putString(ARG_SOCIAL_ID_TOKEN, idToken);
        args.putString(ARG_SOCIAL_SERVICE, service);
        args.putBoolean(ARG_SOCIAL_LOGIN, isSocialLogin);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        mEmailAddress = getArguments().getString(ARG_EMAIL_ADDRESS);
        mPassword = getArguments().getString(ARG_PASSWORD);
        mIdToken = getArguments().getString(ARG_SOCIAL_ID_TOKEN);
        mService = getArguments().getString(ARG_SOCIAL_SERVICE);
        isSocialLogin = getArguments().getBoolean(ARG_SOCIAL_LOGIN);

        if (savedInstanceState != null) {
            mRequestedPassword = savedInstanceState.getString(KEY_REQUESTED_PASSWORD);
        }
    }

    @Override
    protected @LayoutRes
    int getContentLayout() {
        return R.layout.login_email_password_screen;
    }

    @Override
    protected @LayoutRes int getProgressBarText() {
        return R.string.logging_in;
    }

    @Override
    protected void setupLabel(TextView label) {
        label.setText(isSocialLogin ? R.string.enter_wpcom_password_google : R.string.enter_wpcom_password);
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        ((TextView) rootView.findViewById(R.id.login_email)).setText(mEmailAddress);

        mPasswordInput = (WPLoginInputRow) rootView.findViewById(R.id.login_password_row);
        mPasswordInput.addTextChangedListener(this);
        mPasswordInput.setOnEditorCommitListener(this);
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        secondaryButton.setText(R.string.forgot_password);
        secondaryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginListener != null) {
                    mLoginListener.forgotPassword(FORGOT_PASSWORD_URL_WPCOM);
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
        return mPasswordInput.getEditText();
    }

    @Override
    protected void onHelp() {
        if (mLoginListener != null) {
            mLoginListener.helpEmailPasswordScreen(mEmailAddress);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_PASSWORD_FORM_VIEWED);

            if (!TextUtils.isEmpty(mPassword)) {
                mPasswordInput.setText(mPassword);
            } else {
                autoFillFromBuildConfig("DEBUG_DOTCOM_LOGIN_PASSWORD", mPasswordInput.getEditText());
            }
        } else {
            mOldSitesIDs = savedInstanceState.getIntegerArrayList(KEY_OLD_SITES_IDS);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_REQUESTED_PASSWORD, mRequestedPassword);
        outState.putIntegerArrayList(KEY_OLD_SITES_IDS, mOldSitesIDs);
    }

    protected void next() {
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        startProgress();

        mRequestedPassword = mPasswordInput.getEditText().getText().toString();

        mOldSitesIDs = SiteUtils.getCurrentSiteIds(mSiteStore, false);

        AuthenticatePayload payload = new AuthenticatePayload(mEmailAddress, mRequestedPassword);
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
    }

    @Override
    public void OnEditorCommit() {
        mPasswordInput.setError(null);
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
        mPasswordInput.setError(null);
    }

    private void showPasswordError() {
        mPasswordInput.setError(getString(R.string.password_incorrect));
    }

    private void handleAuthError(AccountStore.AuthenticationErrorType error, String errorMessage) {
        switch (error) {
            case INCORRECT_USERNAME_OR_PASSWORD:
            case NOT_AUTHENTICATED: // NOT_AUTHENTICATED is the generic error from XMLRPC response on first call.
                showPasswordError();
                break;
            case NEEDS_2FA:
                // login credentials were correct anyway so, offer to save to SmartLock
                saveCredentialsInSmartLock(mLoginListener.getSmartLockHelper(), mEmailAddress, mPassword);

                if (isSocialLogin) {
                    mLoginListener.needs2faSocialConnect(mEmailAddress, mRequestedPassword, mIdToken, mService);
                } else {
                    mLoginListener.needs2fa(mEmailAddress, mRequestedPassword);
                }

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

        if (isSocialLogin) {
            AccountStore.PushSocialLoginPayload payload = new AccountStore.PushSocialLoginPayload(mIdToken, mService);
            mDispatcher.dispatch(AccountActionBuilder.newPushSocialConnectAction(payload));
        } else {
            saveCredentialsInSmartLock(mLoginListener.getSmartLockHelper(), mEmailAddress, mRequestedPassword);
            doFinishLogin();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocialChanged(OnSocialChanged event) {
        if (event.isError()) {
            switch (event.error.type) {
                case UNABLE_CONNECT:
                    AppLog.e(T.API, "Unable to connect WordPress.com account to social account.");
                    break;
                case USER_ALREADY_ASSOCIATED:
                    AppLog.e(T.API, "This social account is already associated with a WordPress.com account.");
                    break;
                // Ignore other error cases.  The above are the only two we have chosen to log.
            }

            doFinishLogin();
        } else if (!event.requiresTwoStepAuth) {
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
