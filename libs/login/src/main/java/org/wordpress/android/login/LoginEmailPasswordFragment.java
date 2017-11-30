package org.wordpress.android.login;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
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
import org.wordpress.android.login.LoginWpcomService.OnCredentialsOK;
import org.wordpress.android.login.LoginWpcomService.OnLoginStateUpdated;
import org.wordpress.android.login.util.SiteUtils;
import org.wordpress.android.login.widgets.WPLoginInputRow;
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutoForeground;
import org.wordpress.android.util.NetworkUtils;

import java.util.ArrayList;

import dagger.android.support.AndroidSupportInjection;

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
    private boolean mIsSocialLogin;

    private AutoForeground.ServiceEventConnection mServiceEventConnection;

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
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEmailAddress = getArguments().getString(ARG_EMAIL_ADDRESS);
        mPassword = getArguments().getString(ARG_PASSWORD);
        mIdToken = getArguments().getString(ARG_SOCIAL_ID_TOKEN);
        mService = getArguments().getString(ARG_SOCIAL_SERVICE);
        mIsSocialLogin = getArguments().getBoolean(ARG_SOCIAL_LOGIN);

        if (savedInstanceState == null) {
            // cleanup the service state on first appearance
            LoginWpcomService.clearLoginServiceState();
        } else {
            mRequestedPassword = savedInstanceState.getString(KEY_REQUESTED_PASSWORD);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // connect to the Service. We'll receive updates via EventBus.
        mServiceEventConnection = new AutoForeground.ServiceEventConnection(getContext(),
                LoginWpcomService.class, this);

        // install the change listener as late as possible so the UI can be setup (updated from the Service state)
        //  before triggering the state cleanup happening in the change listener.
        mPasswordInput.addTextChangedListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        // disconnect from the Service
        mServiceEventConnection.disconnect(getContext(), this);
    }

    @Override
    protected boolean listenForLogin() {
        return false;
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
    protected void setupLabel(@NonNull TextView label) {
        label.setText(mIsSocialLogin ? R.string.enter_wpcom_password_google : R.string.enter_wpcom_password);
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        ((TextView) rootView.findViewById(R.id.login_email)).setText(mEmailAddress);

        mPasswordInput = rootView.findViewById(R.id.login_password_row);
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
                    mPasswordInput.getEditText().setText(BuildConfig.DEBUG_WPCOM_LOGIN_PASSWORD);
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

        startProgress(false);

        mRequestedPassword = mPasswordInput.getEditText().getText().toString();

        LoginWpcomService.loginWithEmailAndPassword(getContext(), mEmailAddress, mRequestedPassword, mIdToken, mService,
                mIsSocialLogin);
        mOldSitesIDs = SiteUtils.getCurrentSiteIds(mSiteStore, false);
    }

    @Override
    public void onEditorCommit() {
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

        LoginWpcomService.clearLoginServiceState();
    }

    private void showPasswordError() {
        mPasswordInput.setError(getString(R.string.password_incorrect));
    }

    private void showError(String error) {
        mPasswordInput.setError(error);
    }

    @Override
    protected void onLoginFinished() {
        if (mIsSocialLogin) {
            mLoginListener.loggedInViaSocialAccount(mOldSitesIDs);
        } else {
            mLoginListener.loggedInViaPassword(mOldSitesIDs);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCredentialsOK(OnCredentialsOK event) {
        saveCredentialsInSmartLock(mLoginListener, mEmailAddress, mRequestedPassword);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onLoginStateUpdated(OnLoginStateUpdated event) {
        AppLog.i(T.NUX, "Received state: " + event.state.name());

        switch (event.state) {
            case IDLE:
                // nothing special to do, we'll start the service on next()
                break;
            case AUTHENTICATING:
            case SOCIAL_LOGIN:
            case FETCHING_ACCOUNT:
            case FETCHING_SETTINGS:
            case FETCHING_SITES:
                if (!isInProgress()) {
                    startProgress();
                }
                break;
            case FAILURE_EMAIL_WRONG_PASSWORD:
                onLoginFinished(false);
                showPasswordError();
                break;
            case FAILURE_2FA:
                onLoginFinished(false);
                mLoginListener.needs2fa(mEmailAddress, mRequestedPassword);

                // consume the state so we don't relauch the 2FA dialog if user backs up
                LoginWpcomService.clearLoginServiceState();
                break;
            case FAILURE_SOCIAL_2FA:
                onLoginFinished(false);
                mLoginListener.needs2faSocialConnect(mEmailAddress, mRequestedPassword, mIdToken, mService);

                // consume the state so we don't relauch the 2FA dialog if user backs up
                LoginWpcomService.clearLoginServiceState();
                break;
            case FAILURE_FETCHING_ACCOUNT:
                onLoginFinished(false);
                showError(getString(R.string.error_fetch_my_profile));
                break;
            case FAILURE_CANNOT_ADD_DUPLICATE_SITE:
                onLoginFinished(false);
                showError(getString(R.string.cannot_add_duplicate_site));
                break;
            case FAILURE:
                onLoginFinished(false);
                showError(getString(R.string.error_generic));
                break;
            case SUCCESS:
                mLoginListener.trackAnalyticsSignIn(mAccountStore, mSiteStore, true);
                mLoginListener.startPostLoginServices();
                onLoginFinished(true);
                break;
        }
    }
}
