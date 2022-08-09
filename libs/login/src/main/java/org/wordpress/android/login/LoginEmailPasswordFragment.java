package org.wordpress.android.login;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.login.LoginWpcomService.LoginState;
import org.wordpress.android.login.LoginWpcomService.OnCredentialsOK;
import org.wordpress.android.login.util.AvatarHelper;
import org.wordpress.android.login.util.AvatarHelper.AvatarRequestListener;
import org.wordpress.android.login.util.SiteUtils;
import org.wordpress.android.login.widgets.WPLoginInputRow;
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutoForeground;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;

import java.util.ArrayList;

import dagger.android.support.AndroidSupportInjection;

public class LoginEmailPasswordFragment extends LoginBaseFormFragment<LoginListener> implements TextWatcher,
        OnEditorCommitListener {
    private static final String KEY_REQUESTED_PASSWORD = "KEY_REQUESTED_PASSWORD";
    private static final String KEY_OLD_SITES_IDS = "KEY_OLD_SITES_IDS";

    protected static final String ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS";
    protected static final String ARG_PASSWORD = "ARG_PASSWORD";
    protected static final String ARG_SOCIAL_ID_TOKEN = "ARG_SOCIAL_ID_TOKEN";
    protected static final String ARG_SOCIAL_LOGIN = "ARG_SOCIAL_LOGIN";
    protected static final String ARG_SOCIAL_SERVICE = "ARG_SOCIAL_SERVICE";
    protected static final String ARG_ALLOW_MAGIC_LINK = "ARG_ALLOW_MAGIC_LINK";
    protected static final String ARG_VERIFY_MAGIC_LINK_EMAIL = "ARG_VERIFY_MAGIC_LINK_EMAIL";

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
    private boolean mAllowMagicLink;
    private boolean mVerifyMagicLinkEmail;

    private AutoForeground.ServiceEventConnection mServiceEventConnection;

    public static LoginEmailPasswordFragment newInstance(String emailAddress, String password,
                                                         String idToken, String service,
                                                         boolean isSocialLogin) {
        return newInstance(emailAddress, password, idToken, service, isSocialLogin, false, false);
    }

    public static LoginEmailPasswordFragment newInstance(String emailAddress, String password, String idToken,
                                                         String service, boolean isSocialLogin, boolean allowMagicLink,
                                                         boolean verifyMagicLinkEmail) {
        LoginEmailPasswordFragment fragment = new LoginEmailPasswordFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL_ADDRESS, emailAddress);
        args.putString(ARG_PASSWORD, password);
        args.putString(ARG_SOCIAL_ID_TOKEN, idToken);
        args.putString(ARG_SOCIAL_SERVICE, service);
        args.putBoolean(ARG_SOCIAL_LOGIN, isSocialLogin);
        args.putBoolean(ARG_ALLOW_MAGIC_LINK, allowMagicLink);
        args.putBoolean(ARG_VERIFY_MAGIC_LINK_EMAIL, verifyMagicLinkEmail);
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

        if (getArguments() != null) {
            mEmailAddress = getArguments().getString(ARG_EMAIL_ADDRESS);
            mPassword = getArguments().getString(ARG_PASSWORD);
            mIdToken = getArguments().getString(ARG_SOCIAL_ID_TOKEN);
            mService = getArguments().getString(ARG_SOCIAL_SERVICE);
            mIsSocialLogin = getArguments().getBoolean(ARG_SOCIAL_LOGIN);
            mAllowMagicLink = getArguments().getBoolean(ARG_ALLOW_MAGIC_LINK);
            mVerifyMagicLinkEmail = getArguments().getBoolean(ARG_VERIFY_MAGIC_LINK_EMAIL);
        }

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
        mAnalyticsListener.emailPasswordFormScreenResumed();
        updatePrimaryButtonEnabledStatus();

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

    private void updatePrimaryButtonEnabledStatus() {
        String currentPassword = mPasswordInput.getEditText().getText().toString();
        getBottomButton().setEnabled(!currentPassword.trim().isEmpty());
    }

    @Override
    protected boolean listenForLogin() {
        return false;
    }

    @Override
    protected @LayoutRes int getContentLayout() {
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
        // important for accessibility - talkback
        getActivity().setTitle(R.string.selfhosted_site_login_title);

        mPasswordInput = rootView.findViewById(R.id.login_password_row);
        mPasswordInput.setOnEditorCommitListener(this);

        rootView.findViewById(R.id.login_reset_password).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginListener != null) {
                    mLoginListener.forgotPassword(FORGOT_PASSWORD_URL_WPCOM);
                }
            }
        });

        final View divider = rootView.findViewById(R.id.login_button_divider);
        divider.setVisibility(mAllowMagicLink ? View.VISIBLE : View.GONE);

        final Button magicLinkButton = rootView.findViewById(R.id.login_get_email_link);
        magicLinkButton.setVisibility(mAllowMagicLink ? View.VISIBLE : View.GONE);
        magicLinkButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                if (mLoginListener != null) {
                    mAnalyticsListener.trackRequestMagicLinkClick();
                    mLoginListener.useMagicLinkInstead(mEmailAddress, mVerifyMagicLinkEmail);
                }
            }
        });

        final ProgressBar avatarProgressBar = rootView.findViewById(R.id.avatar_progress);
        final ImageView avatarView = rootView.findViewById(R.id.gravatar);
        final TextView emailView = rootView.findViewById(R.id.email);

        emailView.setText(mEmailAddress);

        AvatarHelper.loadAvatarFromEmail(this, mEmailAddress, avatarView, new AvatarRequestListener() {
            @Override public void onRequestFinished() {
                avatarProgressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void buildToolbar(Toolbar toolbar, ActionBar actionBar) {
        actionBar.setTitle(R.string.log_in);
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
            mAnalyticsListener.trackPasswordFormViewed(mIsSocialLogin);

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
        mAnalyticsListener.trackSubmitClicked();

        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        startProgress(false);

        mRequestedPassword = mPasswordInput.getEditText().getText().toString();

        LoginWpcomService.loginWithEmailAndPassword(
                getContext(),
                mEmailAddress,
                mRequestedPassword,
                mIdToken,
                mService,
                mIsSocialLogin,
                mLoginListener.getLoginMode() == LoginMode.JETPACK_LOGIN_ONLY,
                mLoginListener.getLoginMode() == LoginMode.WOO_LOGIN_MODE
        );
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
        updatePrimaryButtonEnabledStatus();
    }

    private void showPasswordError() {
        String message = getString(R.string.password_incorrect);
        mAnalyticsListener.trackFailure(message);
        mPasswordInput.setError(message);
    }

    private void showError(String error) {
        mAnalyticsListener.trackFailure(error);
        mPasswordInput.setError(error);
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

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCredentialsOK(OnCredentialsOK event) {
        saveCredentialsInSmartLock(mLoginListener, mEmailAddress, mRequestedPassword);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onLoginStateUpdated(LoginState loginState) {
        AppLog.i(T.NUX, "Received state: " + loginState.getStepName());

        switch (loginState.getStep()) {
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
            case FAILURE_USE_WPCOM_USERNAME_INSTEAD_OF_EMAIL:
                onLoginFinished(false);
                mLoginListener.loginViaWpcomUsernameInstead();
                ToastUtils.showToast(getContext(), R.string.error_user_username_instead_of_email, Duration.LONG);

                mAnalyticsListener.trackFailure(loginState.getStep().name());
                // consume the state so we don't re-redirect to username login if user backs up
                LoginWpcomService.clearLoginServiceState();
                break;
            case FAILURE:
                onLoginFinished(false);
                showError(getString(R.string.error_generic));
                break;
            case SUCCESS:
                onLoginFinished(true);
                break;
        }
    }
}
