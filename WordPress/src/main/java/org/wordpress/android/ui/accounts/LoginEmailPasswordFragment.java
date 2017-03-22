package org.wordpress.android.ui.accounts;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateService;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class LoginEmailPasswordFragment extends AbstractFragment implements TextWatcher {
    private static String ARG_EMAIL = "email";

    public static final String TAG = "login_email_password_fragment_tag";

    private static final String DOT_COM_BASE_URL = "https://wordpress.com";
    private static final String FORGOT_PASSWORD_RELATIVE_URL = "/wp-login.php?action=lostpassword";
    private static final int WPCOM_ERRONEOUS_LOGIN_THRESHOLD = 3;

    protected String mEmail;
    protected String mPassword;
    protected int mErroneousLogInCount;

    protected TextView mEmailTextView;
    protected EditText mPasswordEditText;
    protected Button mLoginButton;
    protected View mLostPassword;

    protected @Inject SiteStore mSiteStore;
    protected @Inject AccountStore mAccountStore;
    protected @Inject Dispatcher mDispatcher;

    protected boolean mSitesFetched = false;
    protected boolean mAccountSettingsFetched = false;
    protected boolean mAccountFetched = false;

    private OnEmailPasswordLoginInteraction mListener;
    private boolean mIsActivityFinishing;

    public interface OnEmailPasswordLoginInteraction {
        void onEmailPasswordLoginSuccess();
    }

    public static LoginEmailPasswordFragment newInstance(String email) {
        LoginEmailPasswordFragment lepf = new LoginEmailPasswordFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_EMAIL, email);

        lepf.setArguments(bundle);
        return lepf;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.login_email_password_screen, container, false);

        mEmail = getArguments().getString(ARG_EMAIL);

        mEmailTextView = (TextView) rootView.findViewById(R.id.login_email);
        mEmailTextView.setText(mEmail);

        mPasswordEditText = (EditText) rootView.findViewById(R.id.login_password);
        mPasswordEditText.addTextChangedListener(this);

        mLoginButton = (Button) rootView.findViewById(R.id.login_email_next_button);
        mLoginButton.setOnClickListener(mLoginClickListener);

        mLostPassword = rootView.findViewById(R.id.login_lost_password);
        mLostPassword.setOnClickListener(mLostPasswordListener);

        mPasswordEditText.setOnEditorActionListener(mEditorAction);

        autofillFromBuildConfig();

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LoginEmailPasswordFragment.OnEmailPasswordLoginInteraction) {
            mListener = (LoginEmailPasswordFragment.OnEmailPasswordLoginInteraction) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnEmailPasswordInteraction");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /*
     * autofill the username and password from BuildConfig/gradle.properties (developer feature,
     * only enabled for DEBUG releases)
     */
    private void autofillFromBuildConfig() {
        if (!BuildConfig.DEBUG) return;

        String password = (String) WordPress.getBuildConfigValue(getActivity().getApplication(),
                "DEBUG_DOTCOM_LOGIN_PASSWORD");
        if (!TextUtils.isEmpty(password)) {
            mPasswordEditText.setText(password);
            AppLog.d(T.NUX, "Autofilled password from build config");
        }
    }

    private String getForgotPasswordURL() {
        return DOT_COM_BASE_URL + FORGOT_PASSWORD_RELATIVE_URL;
    }

    private final OnClickListener mLostPasswordListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String forgotPasswordUrl = getForgotPasswordURL();
            AppLog.i(T.NUX, "User tapped forgot password link: " + forgotPasswordUrl);
            ActivityLauncher.openUrlExternal(getContext(), forgotPasswordUrl);
        }
    };

    protected void onDoneAction() {
        login();
    }

    private final TextView.OnEditorActionListener mEditorAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            return onDoneEvent(actionId, event);
        }
    };

    private void trackAnalyticsSignIn() {
        AnalyticsUtils.refreshMetadata(mAccountStore, mSiteStore);
        Map<String, Boolean> properties = new HashMap<>();
        properties.put("dotcom_user", true);
        AnalyticsTracker.track(Stat.SIGNED_IN, properties);
    }

    private void finishCurrentActivity() {
        if (mIsActivityFinishing) {
            return;
        }

        mIsActivityFinishing = true;
        if (getActivity() == null) {
            return;
        }

        mListener.onEmailPasswordLoginSuccess();
    }

    private void signInAndFetchBlogListWPCom() {
        startProgress(getString(R.string.connecting_wpcom));
        AuthenticatePayload payload = new AuthenticatePayload(mEmail, mPassword);
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
    }

    private boolean checkNetworkConnectivity() {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            SignInDialogFragment nuxAlert;
            nuxAlert = SignInDialogFragment.newInstance(getString(R.string.no_network_title),
                    getString(R.string.no_network_message),
                    R.drawable.ic_notice_white_64dp,
                    getString(R.string.cancel));
            ft.add(nuxAlert, "alert");
            ft.commitAllowingStateLoss();
            return false;
        }
        return true;
    }

    protected void login() {
        if (!isUserDataValid()) {
            return;
        }

        if (!checkNetworkConnectivity()) {
            return;
        }

        mPassword = EditTextUtils.getText(mPasswordEditText).trim();
        AppLog.i(T.NUX, "User tries to sign in on WordPress.com with email: " + mEmail);
        signInAndFetchBlogListWPCom();
    }

    private final OnClickListener mLoginClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            login();
        }
    };

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (fieldsFilled()) {
            mLoginButton.setEnabled(true);
        } else {
            mLoginButton.setEnabled(false);
        }
        mPasswordEditText.setError(null);
    }

    private boolean fieldsFilled() {
        return EditTextUtils.getText(mPasswordEditText).trim().length() > 0;
    }

    protected boolean isUserDataValid() {
        final String password = EditTextUtils.getText(mPasswordEditText).trim();
        boolean retValue = true;

        if (TextUtils.isEmpty(password)) {
            mPasswordEditText.setError(getString(R.string.required_field));
            mPasswordEditText.requestFocus();
            retValue = false;
        }

        return retValue;
    }

    private void showPasswordError(int messageId) {
        mPasswordEditText.setError(getString(messageId));
        mPasswordEditText.requestFocus();
    }

    protected boolean specificShowError(int messageId) {
        switch (getErrorType(messageId)) {
            case PASSWORD:
                showPasswordError(messageId);
                return true;
            default:
                return false;
        }
    }

    protected void handleInvalidUsernameOrPassword(int messageId) {
        mErroneousLogInCount += 1;
        if (mErroneousLogInCount >= WPCOM_ERRONEOUS_LOGIN_THRESHOLD) {
            // Clear previous errors
            mPasswordEditText.setError(null);
        } else {
            showPasswordError(messageId);
        }
        endProgress();
    }

    private void showGenericErrorDialog(String errorMessage) {
        showGenericErrorDialog(errorMessage, null, null);
    }

    private void showGenericErrorDialog(String errorMessage, String faqId, String faqSection) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        SignInDialogFragment nuxAlert;

        int faqAction = SignInDialogFragment.ACTION_OPEN_SUPPORT_CHAT;
        String thirdButtonLabel = getString(R.string.contact_us);
        if (!TextUtils.isEmpty(faqId) || !TextUtils.isEmpty(faqSection)) {
            faqAction = SignInDialogFragment.ACTION_OPEN_FAQ_PAGE;
            thirdButtonLabel =  getString(R.string.tell_me_more);
        }
        nuxAlert = SignInDialogFragment.newInstance(getString(R.string.nux_cannot_log_in),
                errorMessage, R.drawable.ic_notice_white_64dp, 3,
                getString(R.string.cancel), getString(R.string.reader_title_applog), thirdButtonLabel,
                SignInDialogFragment.ACTION_OPEN_SUPPORT_CHAT,
                SignInDialogFragment.ACTION_OPEN_APPLICATION_LOG,
                faqAction, faqId, faqSection);
        Bundle bundle = nuxAlert.getArguments();
        bundle.putSerializable(HelpshiftHelper.ORIGIN_KEY, HelpshiftHelper.chooseHelpshiftLoginTag(true, true));
        nuxAlert.setArguments(bundle);
        ft.add(nuxAlert, "alert");
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Autofill username / password if string fields are set (only useful after an error in sign up).
        // This can't be done in onCreateView
        if (mEmail != null) {
            mEmailTextView.setText(mEmail);
        }
        if (mPassword != null) {
            mPasswordEditText.setText(mPassword);
        }
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mDispatcher.unregister(this);
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (event.isError()) {
            AppLog.e(T.API, "onAccountChanged has error: " + event.error.type + " - " + event.error.message);
            showAccountError(event.error.type, event.error.message);
            endProgress();
            return;
        }

        AppLog.i(T.NUX, "onAccountChanged: " + event.toString());

        // Success
        mAccountSettingsFetched |= event.causeOfChange == AccountAction.FETCH_SETTINGS;
        mAccountFetched |= event.causeOfChange == AccountAction.FETCH_ACCOUNT;

        // Finish activity if sites have been fetched
        if (mSitesFetched && mAccountSettingsFetched && mAccountFetched) {
            finishCurrentActivity();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            AppLog.e(T.API, "onAuthenticationChanged has error: " + event.error.type + " - " + event.error.message);
            AnalyticsTracker.track(Stat.LOGIN_FAILED, event.getClass().getSimpleName(), event.error.type.toString(), event.error.message);

            showAuthError(event.error.type, event.error.message);
            endProgress();
            return;
        }

        AppLog.i(T.NUX, "onAuthenticationChanged: " + event.toString());

        fetchAccountSettingsAndSites();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        AppLog.i(T.NUX, "onSiteChanged: " + event.toString());

        if (event.isError()) {
            AppLog.e(T.API, "onSiteChanged has error: " + event.error.type + " - " + event.error.toString());
            endProgress();
            if (!isAdded()) {
                return;
            }
            if (event.error.type == SiteErrorType.DUPLICATE_SITE) {
                if (event.rowsAffected == 0) {
                    // If there is a duplicate site and not any site has been added, show an error and
                    // stop the sign in process
                    ToastUtils.showToast(getContext(), R.string.cannot_add_duplicate_site);
                    return;
                } else {
                    // If there is a duplicate site, notify the user something could be wrong,
                    // but continue the sign in process
                    ToastUtils.showToast(getContext(), R.string.duplicate_site_detected);
                }
            } else {
                return;
            }
        }

        // Login Successful
        trackAnalyticsSignIn();
        mSitesFetched = true;

        // Finish activity if account settings have been fetched or if it's a wporg site
        if ((mAccountSettingsFetched && mAccountFetched)) {
            finishCurrentActivity();
        }
    }

    private void showAccountError(AccountStore.AccountErrorType error, String errorMessage) {
        switch (error) {
            case ACCOUNT_FETCH_ERROR:
                showError(R.string.error_fetch_my_profile);
                break;
            case SETTINGS_FETCH_ERROR:
                showError(R.string.error_fetch_account_settings);
                break;
            case SETTINGS_POST_ERROR:
                showError(R.string.error_post_account_settings);
                break;
            case GENERIC_ERROR:
            default:
                showError(errorMessage);
                break;
        }
    }

    private void showAuthError(AuthenticationErrorType error, String errorMessage) {
        switch (error) {
            case INCORRECT_USERNAME_OR_PASSWORD:
            case NOT_AUTHENTICATED: // NOT_AUTHENTICATED is the generic error from XMLRPC response on first call.
                handleInvalidUsernameOrPassword(R.string.username_or_password_incorrect);
                break;
            case INVALID_OTP:
//                showTwoStepCodeError(R.string.invalid_verification_code);
                break;
            case NEEDS_2FA:
//                setTwoStepAuthVisibility(true);
//                mTwoStepEditText.setText(getAuthCodeFromClipboard());
                break;
            case INVALID_REQUEST:
                // TODO: FluxC: could be specific?
            default:
                // For all other kind of error, show a dialog with API Response error message
                AppLog.e(T.NUX, "Server response: " + errorMessage);
                showGenericErrorDialog(errorMessage);
                break;
        }
    }

    private void fetchAccountSettingsAndSites() {
        if (mAccountStore.hasAccessToken()) {
            // Fetch user infos
            mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
            mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
            // Fetch sites
            mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
            // Start Notification service
            NotificationsUpdateService.startService(getActivity().getApplicationContext());
        }
    }
}
