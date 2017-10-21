package org.wordpress.android.login;

import android.content.Context;
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
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.login.util.SiteUtils;
import org.wordpress.android.login.widgets.WPLoginInputRow;
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;

import dagger.android.support.AndroidSupportInjection;

public class LoginEmailPasswordFragment extends LoginBaseFormFragment<LoginListener> implements TextWatcher,
        OnEditorCommitListener {
    private static final String KEY_REQUESTED_PASSWORD = "KEY_REQUESTED_PASSWORD";
    private static final String KEY_OLD_SITES_IDS = "KEY_OLD_SITES_IDS";

    private static final String ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS";
    private static final String ARG_PASSWORD = "ARG_PASSWORD";

    private static final String FORGOT_PASSWORD_URL_WPCOM = "https://wordpress.com/";

    public static final String TAG = "login_email_password_fragment_tag";

    private WPLoginInputRow mPasswordInput;

    private String mRequestedPassword;
    ArrayList<Integer> mOldSitesIDs;

    private String mEmailAddress;
    private String mPassword;

    public static LoginEmailPasswordFragment newInstance(String emailAddress, String password) {
        LoginEmailPasswordFragment fragment = new LoginEmailPasswordFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL_ADDRESS, emailAddress);
        args.putString(ARG_PASSWORD, password);
        fragment.setArguments(args);
        return fragment;
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
        label.setText(R.string.enter_wpcom_password);
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
            mLoginListener.track(AnalyticsTracker.Stat.LOGIN_PASSWORD_FORM_VIEWED);

            if (!TextUtils.isEmpty(mPassword)) {
                mPasswordInput.setText(mPassword);
            } else {
                if (BuildConfig.DEBUG) {
                    mPasswordInput.getEditText().setText(BuildConfig.DEBUG_DOTCOM_LOGIN_PASSWORD);
                }
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
                saveCredentialsInSmartLock(mLoginListener, mEmailAddress, mPassword);

                mLoginListener.needs2fa(mEmailAddress, mRequestedPassword);
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

        saveCredentialsInSmartLock(mLoginListener, mEmailAddress, mRequestedPassword);

        doFinishLogin();
    }

    @Override
    protected void onLoginFinished() {
        mLoginListener.trackAnalyticsSignIn(mAccountStore, mSiteStore, true);

        mLoginListener.loggedInViaPassword(mOldSitesIDs);
    }
}
