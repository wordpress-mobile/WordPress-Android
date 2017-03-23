package org.wordpress.android.ui.accounts.login;

import com.google.android.gms.auth.api.credentials.Credential;

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
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.IsAvailable;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAvailabilityChecked;
import org.wordpress.android.fluxc.store.AccountStore.OnDiscoveryResponse;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.accounts.AbstractFragment;
import org.wordpress.android.ui.accounts.NewUserFragment;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.accounts.SignInDialogFragment;
import org.wordpress.android.ui.accounts.SmartLockHelper;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateService;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SelfSignedSSLUtils;
import org.wordpress.android.util.SelfSignedSSLUtils.Callback;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPUrlUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

public class LoginUsernamePasswordFragment extends AbstractFragment implements TextWatcher {
    public static final String TAG = "login_username_password_fragment_tag";

    private static final String ARG_SITE_ADDRESS = "arg_site_address";
    private static final String ARG_IS_SELF_HOSTED = "arg_is_self_hosted";

    private static final String DOT_COM_BASE_URL = "https://wordpress.com";
    private static final String FORGOT_PASSWORD_RELATIVE_URL = "/wp-login.php?action=lostpassword";
    private static final int WPCOM_ERRONEOUS_LOGIN_THRESHOLD = 3;
    private static final String KEY_IS_SELF_HOSTED = "IS_SELF_HOSTED";

    public static final String ENTERED_URL_KEY = "ENTERED_URL_KEY";
    public static final String ENTERED_USERNAME_KEY = "ENTERED_USERNAME_KEY";

    private static final String XMLRPC_BLOCKED_HELPSHIFT_FAQ_SECTION = "10";
    private static final String XMLRPC_BLOCKED_HELPSHIFT_FAQ_ID = "102";

    private static final String MISSING_XMLRPC_METHOD_HELPSHIFT_FAQ_SECTION = "10";
    private static final String MISSING_XMLRPC_METHOD_HELPSHIFT_FAQ_ID = "11";

    private static final String NO_SITE_HELPSHIFT_FAQ_SECTION = "10";
    private static final String NO_SITE_HELPSHIFT_FAQ_ID = "2"; //using the same as in INVALID URL

    protected EditText mUsernameEditText;
    protected EditText mPasswordEditText;

    protected String mSiteAddress;
    protected boolean mSelfHosted;
    protected int mErroneousLogInCount;
    protected String mUsername;
    protected String mPassword;

    protected Button mLoginButton;

    protected View mLostPassword;

    protected @Inject SiteStore mSiteStore;
    protected @Inject AccountStore mAccountStore;
    protected @Inject Dispatcher mDispatcher;
    protected @Inject HTTPAuthManager mHTTPAuthManager;
    protected @Inject MemorizingTrustManager mMemorizingTrustManager;

    protected boolean mSitesFetched = false;
    protected boolean mAccountSettingsFetched = false;
    protected boolean mAccountFetched = false;

    private OnLoginUsernamePasswordInteraction mListener;

    private boolean mSmartLockEnabled = true;
    private boolean mIsActivityFinishing;

    public interface OnLoginUsernamePasswordInteraction {
        void onUsernamePasswordLoginSuccess();
    }

    public static LoginUsernamePasswordFragment newInstance(String siteAddress, boolean isSelfHosted) {
        LoginUsernamePasswordFragment fragment = new LoginUsernamePasswordFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SITE_ADDRESS, siteAddress);
        args.putBoolean(ARG_IS_SELF_HOSTED, isSelfHosted);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (getArguments() != null) {
            mSiteAddress = getArguments().getString(ARG_SITE_ADDRESS);
            mSelfHosted = getArguments().getBoolean(ARG_IS_SELF_HOSTED);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.login_site_username_password_screen, container, false);

        mUsernameEditText = (EditText) rootView.findViewById(R.id.username);
        mUsernameEditText.addTextChangedListener(this);
        mPasswordEditText = (EditText) rootView.findViewById(R.id.password);
        mPasswordEditText.addTextChangedListener(this);

        mLoginButton = (Button) rootView.findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(mSignInClickListener);

        mLostPassword = rootView.findViewById(R.id.lost_password);
        mLostPassword.setOnClickListener(mLostPasswordListener);

        mPasswordEditText.setOnEditorActionListener(mEditorAction);

        autofillFromBuildConfig();

        mUsernameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((didPressNextKey(actionId, event) || didPressEnterKey(actionId, event))) {
                    login();
                    return true;
                } else {
                    return false;
                }
            }
        });
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LoginUsernamePasswordFragment.OnLoginUsernamePasswordInteraction) {
            mListener = (LoginUsernamePasswordFragment.OnLoginUsernamePasswordInteraction) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnLoginUsernamePasswordInteraction");
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

        String userName = (String) WordPress.getBuildConfigValue(getActivity().getApplication(),
                "DEBUG_DOTCOM_LOGIN_USERNAME");
        String password = (String) WordPress.getBuildConfigValue(getActivity().getApplication(),
                "DEBUG_DOTCOM_LOGIN_PASSWORD");
        if (!TextUtils.isEmpty(userName)) {
            mUsernameEditText.setText(userName);
            AppLog.d(T.NUX, "Autofilled username from build config");
        }
        if (!TextUtils.isEmpty(password)) {
            mPasswordEditText.setText(password);
            AppLog.d(T.NUX, "Autofilled password from build config");
        }
    }

    public boolean canAutofillUsernameAndPassword() {
        return EditTextUtils.getText(mUsernameEditText).isEmpty()
               && EditTextUtils.getText(mPasswordEditText).isEmpty()
               && mUsernameEditText != null
               && mPasswordEditText != null
               && mSmartLockEnabled
               && !mSelfHosted;
    }

    public void onCredentialRetrieved(Credential credential) {
        AppLog.d(T.NUX, "Retrieved username from SmartLock: " + credential.getId());
        if (isAdded() && canAutofillUsernameAndPassword()) {
            AnalyticsTracker.track(Stat.LOGIN_AUTOFILL_CREDENTIALS_FILLED);
            mUsernameEditText.setText(credential.getId());
            mPasswordEditText.setText(credential.getPassword());
        }
    }

    private boolean isWPComLogin() {
        return !mSelfHosted || TextUtils.isEmpty(mSiteAddress) ||
                WPUrlUtils.isWordPressCom(UrlUtils.addUrlSchemeIfNeeded(mSiteAddress, false));
    }

    private String getForgotPasswordURL() {
        String baseUrl = DOT_COM_BASE_URL;
        if (!isWPComLogin()) {
            baseUrl = mSiteAddress.trim();
            String lowerCaseBaseUrl = baseUrl.toLowerCase(Locale.getDefault());
            if (!lowerCaseBaseUrl.startsWith("https://") && !lowerCaseBaseUrl.startsWith("http://")) {
                baseUrl = "http://" + baseUrl;
            }
        }
        return baseUrl + FORGOT_PASSWORD_RELATIVE_URL;
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
            if (mPasswordEditText == v) {
                return onDoneEvent(actionId, event);
            }
            return false;
        }
    };

    private void trackAnalyticsSignIn() {
        AnalyticsUtils.refreshMetadata(mAccountStore, mSiteStore);
        Map<String, Boolean> properties = new HashMap<>();
        properties.put("dotcom_user", isWPComLogin());
        AnalyticsTracker.track(Stat.SIGNED_IN, properties);
        if (!isWPComLogin()) {
            AnalyticsTracker.track(Stat.ADDED_SELF_HOSTED_SITE);
        }
    }

    private void saveCredentialsInSmartLock() {
        SmartLockHelper smartLockHelper = getSmartLockHelper();
        // mUsername and mPassword are null when the user log in with a magic link
        if (smartLockHelper != null && mUsername != null && mPassword != null) {
            smartLockHelper.saveCredentialsInSmartLock(mUsername, mPassword,
                    HtmlUtils.fastUnescapeHtml(mAccountStore.getAccount().getDisplayName()),
                    Uri.parse(mAccountStore.getAccount().getAvatarUrl()));
        }
    }

    private void finishCurrentActivity() {
        if (mIsActivityFinishing) {
            return;
        }

        mIsActivityFinishing = true;
        saveCredentialsInSmartLock();
        if (getActivity() == null) {
            return;
        }

        mListener.onUsernamePasswordLoginSuccess();
    }

    private SmartLockHelper getSmartLockHelper() {
        if (getActivity() != null && getActivity() instanceof SignInActivity) {
            return ((SignInActivity) getActivity()).getSmartLockHelper();
        }
        return null;
    }

    private void signInAndFetchBlogListWPCom() {
        startProgress(getString(R.string.connecting_wpcom));
        AuthenticatePayload payload = new AuthenticatePayload(mUsername, mPassword);
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
    }

    private void signInAndFetchBlogListWPOrg() {
        startProgress(getString(R.string.signing_in));
        // Self Hosted don't have any "Authentication" request, try to list sites with user/password
        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mSiteAddress));
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

        mUsername = EditTextUtils.getText(mUsernameEditText).trim().toLowerCase();
        mPassword = EditTextUtils.getText(mPasswordEditText).trim();

        if (mSelfHosted) {
            AppLog.i(T.NUX, "User tries to sign in on Self Hosted: " + mSiteAddress
                            + " with username: " + mUsername);
            signInAndFetchBlogListWPOrg();
        } else {
            signInAndFetchBlogListWPCom();
        }
    }

    private final OnClickListener mSignInClickListener = new OnClickListener() {
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
        mUsernameEditText.setError(null);
    }

    private boolean fieldsFilled() {
        return EditTextUtils.getText(mUsernameEditText).trim().length() > 0
               && EditTextUtils.getText(mPasswordEditText).trim().length() > 0;
    }

    protected boolean isUserDataValid() {
        final String username = EditTextUtils.getText(mUsernameEditText).trim();
        final String password = EditTextUtils.getText(mPasswordEditText).trim();
        boolean retValue = true;

        if (TextUtils.isEmpty(password)) {
            mPasswordEditText.setError(getString(R.string.required_field));
            mPasswordEditText.requestFocus();
            retValue = false;
        }

        if (TextUtils.isEmpty(username)) {
            mUsernameEditText.setError(getString(R.string.required_field));
            mUsernameEditText.requestFocus();
            retValue = false;
        }

        return retValue;
    }

    private void showPasswordError(int messageId) {
        mPasswordEditText.setError(getString(messageId));
        mPasswordEditText.requestFocus();
    }

    private void showUsernameError(int messageId) {
        mUsernameEditText.setError(getString(messageId));
        mUsernameEditText.requestFocus();
    }

    protected boolean specificShowError(int messageId) {
        switch (getErrorType(messageId)) {
            case USERNAME:
            case PASSWORD:
                showPasswordError(messageId);
                showUsernameError(messageId);
                return true;
            default:
                return false;
        }
    }

    protected void showInvalidUsernameOrPasswordDialog() {
        // Show a dialog
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        SignInDialogFragment nuxAlert;
        // create a 3 buttons dialog ("Contact us", "Forget your password?" and "Cancel")
        nuxAlert = SignInDialogFragment.newInstance(getString(R.string.nux_cannot_log_in),
                getString(R.string.username_or_password_incorrect),
                R.drawable.ic_notice_white_64dp, 3, getString(
                        R.string.cancel), getString(
                        R.string.forgot_password), getString(
                        R.string.contact_us), SignInDialogFragment.ACTION_OPEN_URL,
                SignInDialogFragment.ACTION_OPEN_SUPPORT_CHAT);

        // Put entered url and entered username args, that could help our support team
        Bundle bundle = nuxAlert.getArguments();
        bundle.putString(SignInDialogFragment.ARG_OPEN_URL_PARAM, getForgotPasswordURL());
        bundle.putString(ENTERED_URL_KEY, mSiteAddress);
        bundle.putString(ENTERED_USERNAME_KEY, EditTextUtils.getText(mUsernameEditText));
        bundle.putSerializable(HelpshiftHelper.ORIGIN_KEY, HelpshiftHelper.chooseHelpshiftLoginTag
                (false, isWPComLogin() && !mSelfHosted));
        nuxAlert.setArguments(bundle);
        ft.add(nuxAlert, "alert");
        ft.commitAllowingStateLoss();
    }

    protected void handleInvalidUsernameOrPassword(int messageId) {
        mErroneousLogInCount += 1;
        if (mErroneousLogInCount >= WPCOM_ERRONEOUS_LOGIN_THRESHOLD) {
            // Clear previous errors
            mPasswordEditText.setError(null);
            mUsernameEditText.setError(null);
            showInvalidUsernameOrPasswordDialog();
        } else {
            showPasswordError(messageId);
            showUsernameError(messageId);
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
        bundle.putSerializable(HelpshiftHelper.ORIGIN_KEY, HelpshiftHelper.chooseHelpshiftLoginTag
                (false, isWPComLogin() && !mSelfHosted));
        nuxAlert.setArguments(bundle);
        ft.add(nuxAlert, "alert");
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_SELF_HOSTED, mSelfHosted);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Autofill username / password if string fields are set (only useful after an error in sign up).
        // This can't be done in onCreateView
        if (mUsername != null) {
            mUsernameEditText.setText(mUsername);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NewUserFragment.NEW_USER && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                // Text views will be populated by username/password if these fields are set
                mUsername = data.getStringExtra("username");
                mPassword = data.getStringExtra("password");
            }
        }
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
        if ((mAccountSettingsFetched && mAccountFetched) || !isWPComLogin()) {
            finishCurrentActivity();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDiscoverySucceeded(OnDiscoveryResponse event) {
        if (event.isError()) {
            AppLog.e(T.API, "onDiscoveryResponse has error: " + event.error.name() + " - " + event.error.toString());
            handleDiscoveryError(event.error, event.failedEndpoint);
            AnalyticsTracker.track(Stat.LOGIN_FAILED, event.getClass().getSimpleName(), event.error.name(), event.error.toString());
            return;
        }
        AppLog.i(T.NUX, "Discovery succeeded, endpoint: " + event.xmlRpcEndpoint);
        RefreshSitesXMLRPCPayload selfhostedPayload = new RefreshSitesXMLRPCPayload();
        selfhostedPayload.username = mUsername;
        selfhostedPayload.password = mPassword;
        selfhostedPayload.url = event.xmlRpcEndpoint;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(selfhostedPayload));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAvailabilityChecked(OnAvailabilityChecked event) {
        if (event.isError()) {
            AppLog.e(T.API, "OnAvailabilityChecked has error: " + event.error.type + " - " + event.error.message);
        }

        switch(event.type) {
            case EMAIL:
                handleEmailAvailabilityEvent(event);
                break;
            case USERNAME:
                handleUsernameAvailabilityEvent(event);
                break;
            default:
                break;
        }
    }

    /**
     * Handler for an email availability event. If a user enters an email address for their
     * username an API checks to see if it belongs to a wpcom account.  If it exists the magic links
     * flow is followed. Otherwise the self-hosted sign in form is shown.
     * @param event
     */
    private void handleEmailAvailabilityEvent(OnAvailabilityChecked event) {
        if (event.type != IsAvailable.EMAIL) {
            return;
        }
        if (!event.isAvailable) {
            login();
        }
    }

    /**
     * Handler for a username availability event. If a user enters a wpcom domain as their username
     * an API call is made to check if the subdomain is a valid username. This method handles
     * the result of that API call, showing an error if the subdomain was not a valid username,
     * or showing the password field if it was a valid username.
     *
     * @param event
     */
    private void handleUsernameAvailabilityEvent(OnAvailabilityChecked event) {
        endProgress();
        if (event.type != IsAvailable.USERNAME ) {
            return;
        }
        if (event.isAvailable) {
            // Username doesn't exist in WordPress.com, show just show an error.
            showUsernameError(R.string.username_invalid);
            return;
        }
        // Username exists in WordPress.com. Update the form and show the password field.
        mUsername = event.value;
        mUsernameEditText.setText(event.value);
    }

    public void handleDiscoveryError(DiscoveryError error, final String failedEndpoint) {
        AppLog.e(T.API, "Discover error: " + error);
        endProgress();
        if (!isAdded()) {
            return;
        }
        switch (error) {
            case ERRONEOUS_SSL_CERTIFICATE:
                SelfSignedSSLUtils.showSSLWarningDialog(getActivity(), mMemorizingTrustManager,
                        new Callback() {
                            @Override
                            public void certificateTrusted() {
                                if (failedEndpoint == null) {
                                    return;
                                }
                                // retry login with the same parameters
                                startProgress(getString(R.string.signing_in));
                                mDispatcher.dispatch(
                                        AuthenticationActionBuilder.newDiscoverEndpointAction(failedEndpoint));
                            }
                        });
                break;
            case HTTP_AUTH_REQUIRED:
//                askForHttpAuthCredentials(failedEndpoint);
                break;
            case WORDPRESS_COM_SITE:
                signInAndFetchBlogListWPCom();
                break;
            case NO_SITE_ERROR:
                showGenericErrorDialog(getResources().getString(R.string.no_site_error),
                        NO_SITE_HELPSHIFT_FAQ_ID,
                        NO_SITE_HELPSHIFT_FAQ_SECTION);
                break;
            case MISSING_XMLRPC_METHOD:
                showGenericErrorDialog(getResources().getString(R.string.xmlrpc_missing_method_error),
                        MISSING_XMLRPC_METHOD_HELPSHIFT_FAQ_ID,
                        MISSING_XMLRPC_METHOD_HELPSHIFT_FAQ_SECTION);
                break;
            case XMLRPC_BLOCKED:
                // use this to help the user a bit:  pass the Helpshift page ID or section ID
                // on the rest of the error cases in this switch
                showGenericErrorDialog(getResources().getString(R.string.xmlrpc_post_blocked_error),
                        XMLRPC_BLOCKED_HELPSHIFT_FAQ_ID,
                        XMLRPC_BLOCKED_HELPSHIFT_FAQ_SECTION);
                break;
            case XMLRPC_FORBIDDEN:
                showGenericErrorDialog(getResources().getString(R.string.xmlrpc_endpoint_forbidden_error),
                        XMLRPC_BLOCKED_HELPSHIFT_FAQ_ID,
                        XMLRPC_BLOCKED_HELPSHIFT_FAQ_SECTION);
                break;
            case GENERIC_ERROR:
            default:
                showGenericErrorDialog(getResources().getString(R.string.nux_cannot_log_in));
                break;
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
