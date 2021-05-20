package org.wordpress.android.login;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnProfileFetched;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;
import org.wordpress.android.login.util.SiteUtils;
import org.wordpress.android.login.widgets.WPLoginInputRow;
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.ArrayList;
import java.util.List;

import dagger.android.support.AndroidSupportInjection;

public class LoginUsernamePasswordFragment extends LoginBaseDiscoveryFragment implements TextWatcher,
        OnEditorCommitListener, LoginBaseDiscoveryFragment.LoginBaseDiscoveryListener {
    private static final String KEY_LOGIN_FINISHED = "KEY_LOGIN_FINISHED";
    private static final String KEY_LOGIN_STARTED = "KEY_LOGIN_STARTED";
    private static final String KEY_REQUESTED_USERNAME = "KEY_REQUESTED_USERNAME";
    private static final String KEY_REQUESTED_PASSWORD = "KEY_REQUESTED_PASSWORD";
    private static final String KEY_OLD_SITES_IDS = "KEY_OLD_SITES_IDS";
    private static final String KEY_GET_SITE_OPTIONS_INITIATED = "KEY_GET_SITE_OPTIONS_INITIATED";

    private static final String ARG_INPUT_SITE_ADDRESS = "ARG_INPUT_SITE_ADDRESS";
    private static final String ARG_ENDPOINT_ADDRESS = "ARG_ENDPOINT_ADDRESS";
    private static final String ARG_INPUT_USERNAME = "ARG_INPUT_USERNAME";
    private static final String ARG_INPUT_PASSWORD = "ARG_INPUT_PASSWORD";
    private static final String ARG_IS_WPCOM = "ARG_IS_WPCOM";

    private static final String FORGOT_PASSWORD_URL_WPCOM = "https://wordpress.com/";

    public static final String TAG = "login_username_password_fragment_tag";

    private NestedScrollView mScrollView;
    private WPLoginInputRow mUsernameInput;
    private WPLoginInputRow mPasswordInput;

    private boolean mAuthFailed;
    private boolean mLoginFinished;
    private boolean mLoginStarted;

    private String mRequestedUsername;
    private String mRequestedPassword;
    ArrayList<Integer> mOldSitesIDs;
    private boolean mGetSiteOptionsInitiated;

    private String mInputSiteAddress;
    private String mInputSiteAddressWithoutSuffix;
    private String mEndpointAddress;
    private String mInputUsername;
    private String mInputPassword;
    private boolean mIsWpcom;

    public static LoginUsernamePasswordFragment newInstance(String inputSiteAddress, String endpointAddress,
                                                            String inputUsername, String inputPassword,
                                                            boolean isWpcom) {
        LoginUsernamePasswordFragment fragment = new LoginUsernamePasswordFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INPUT_SITE_ADDRESS, inputSiteAddress);
        args.putString(ARG_ENDPOINT_ADDRESS, endpointAddress);
        args.putString(ARG_INPUT_USERNAME, inputUsername);
        args.putString(ARG_INPUT_PASSWORD, inputPassword);
        args.putBoolean(ARG_IS_WPCOM, isWpcom);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.login_username_password_screen;
    }

    @Override
    protected @LayoutRes int getProgressBarText() {
        return R.string.logging_in;
    }

    @Override
    protected void setupLabel(@NonNull TextView label) {
        final boolean isWoo = mLoginListener.getLoginMode() == LoginMode.WOO_LOGIN_MODE;
        final int labelResId = isWoo ? R.string.enter_credentials_for_site : R.string.enter_account_info_for_site;
        final String siteAddress =
                (mEndpointAddress == null || mEndpointAddress.isEmpty()) ? mInputSiteAddress : mEndpointAddress;
        final String formattedSiteAddress =
                UrlUtils.removeScheme(UrlUtils.removeXmlrpcSuffix(StringUtils.notNullStr(siteAddress)));
        label.setText(getString(labelResId, formattedSiteAddress));
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        // important for accessibility - talkback
        getActivity().setTitle(R.string.selfhosted_site_login_title);
        mScrollView = rootView.findViewById(R.id.scroll_view);

        mInputSiteAddressWithoutSuffix = (mEndpointAddress == null || mEndpointAddress.isEmpty())
                ? mInputSiteAddress : UrlUtils.removeXmlrpcSuffix(mEndpointAddress);

        mUsernameInput = rootView.findViewById(R.id.login_username_row);
        mUsernameInput.setText(mInputUsername);
        if (BuildConfig.DEBUG && mInputUsername == null) {
            mUsernameInput.getEditText().setText(BuildConfig.DEBUG_WPCOM_LOGIN_USERNAME);
        }
        mUsernameInput.addTextChangedListener(this);
        mUsernameInput.setOnEditorCommitListener(new OnEditorCommitListener() {
            @Override
            public void onEditorCommit() {
                showError(null);
                mPasswordInput.getEditText().requestFocus();
            }
        });

        mPasswordInput = rootView.findViewById(R.id.login_password_row);
        mPasswordInput.setText(mInputPassword);
        if (BuildConfig.DEBUG && mInputPassword == null) {
            mPasswordInput.getEditText().setText(BuildConfig.DEBUG_WPCOM_LOGIN_PASSWORD);
        }
        mPasswordInput.addTextChangedListener(this);

        mPasswordInput.setOnEditorCommitListener(this);

        rootView.findViewById(R.id.login_reset_password).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginListener != null) {
                    if (mIsWpcom) {
                        mLoginListener.forgotPassword(FORGOT_PASSWORD_URL_WPCOM);
                    } else {
                        if (!mInputSiteAddressWithoutSuffix.endsWith("/")) {
                            mInputSiteAddressWithoutSuffix += "/";
                        }
                        mLoginListener.forgotPassword(mInputSiteAddressWithoutSuffix);
                    }
                }
            }
        });
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
        return mUsernameInput.getEditText();
    }

    @Override
    protected void onHelp() {
        if (mLoginListener != null) {
            mLoginListener.helpUsernamePassword(mInputSiteAddress, mRequestedUsername, mIsWpcom);
        }
    }

    @Override public void onDestroyView() {
        if (mPasswordInput != null) {
            mPasswordInput.setOnEditorCommitListener(null);
            mPasswordInput = null;
        }
        if (mUsernameInput != null) {
            mUsernameInput.setOnEditorCommitListener(null);
            mUsernameInput = null;
        }
        mScrollView = null;

        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInputSiteAddress = getArguments().getString(ARG_INPUT_SITE_ADDRESS);
        mEndpointAddress = getArguments().getString(ARG_ENDPOINT_ADDRESS, null);
        mInputUsername = getArguments().getString(ARG_INPUT_USERNAME);
        mInputPassword = getArguments().getString(ARG_INPUT_PASSWORD);
        mIsWpcom = getArguments().getBoolean(ARG_IS_WPCOM);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mLoginFinished = savedInstanceState.getBoolean(KEY_LOGIN_FINISHED);
            mLoginStarted = savedInstanceState.getBoolean(KEY_LOGIN_STARTED);

            mRequestedUsername = savedInstanceState.getString(KEY_REQUESTED_USERNAME);
            mRequestedPassword = savedInstanceState.getString(KEY_REQUESTED_PASSWORD);
            mOldSitesIDs = savedInstanceState.getIntegerArrayList(KEY_OLD_SITES_IDS);
            mGetSiteOptionsInitiated = savedInstanceState.getBoolean(KEY_GET_SITE_OPTIONS_INITIATED);
        } else {
            mAnalyticsListener.trackUsernamePasswordFormViewed();

            // auto-login if username and password are set for wpcom login
            if (mIsWpcom && !TextUtils.isEmpty(mInputUsername) && !TextUtils.isEmpty(mInputPassword)) {
                getBottomButton().post(new Runnable() {
                    @Override
                    public void run() {
                        getBottomButton().performClick();
                    }
                });
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_LOGIN_FINISHED, mLoginFinished);
        outState.putBoolean(KEY_LOGIN_STARTED, mLoginStarted);
        outState.putString(KEY_REQUESTED_USERNAME, mRequestedUsername);
        outState.putString(KEY_REQUESTED_PASSWORD, mRequestedPassword);
        outState.putIntegerArrayList(KEY_OLD_SITES_IDS, mOldSitesIDs);
        outState.putBoolean(KEY_GET_SITE_OPTIONS_INITIATED, mGetSiteOptionsInitiated);
    }

    @Override public void onResume() {
        super.onResume();
        mAnalyticsListener.usernamePasswordScreenResumed();
        updatePrimaryButtonEnabledStatus();
    }

    private void updatePrimaryButtonEnabledStatus() {
        String currentUsername = mUsernameInput.getEditText().getText().toString();
        String currentPassword = mPasswordInput.getEditText().getText().toString();
        getBottomButton().setEnabled(!currentPassword.trim().isEmpty() && !currentUsername.trim().isEmpty());
    }

    protected void next() {
        mAnalyticsListener.trackSubmitClicked();
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        if (TextUtils.isEmpty(getCleanedUsername())) {
            showUsernameError(getString(R.string.login_empty_username));
            EditTextUtils.showSoftInput(mUsernameInput.getEditText());
            return;
        }

        final String password = mPasswordInput.getEditText().getText().toString();

        if (TextUtils.isEmpty(password)) {
            showPasswordError(getString(R.string.login_empty_password));
            EditTextUtils.showSoftInput(mPasswordInput.getEditText());
            return;
        }

        mLoginStarted = true;
        startProgress();

        mRequestedUsername = getCleanedUsername();
        mRequestedPassword = password;

        // clear up the authentication-failed flag before
        mAuthFailed = false;

        mOldSitesIDs = SiteUtils.getCurrentSiteIds(mSiteStore, false);

        if (mIsWpcom) {
            AuthenticatePayload payload = new AuthenticatePayload(mRequestedUsername, mRequestedPassword);
            mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        } else if (mLoginListener.getLoginMode() == LoginMode.WOO_LOGIN_MODE
                   && (mEndpointAddress == null || mEndpointAddress.isEmpty())) {
            // mEndpointAddress will only be null/empty when redirecting from the Woo login flow
            // initiate the discovery process before fetching the xmlrpc site
            mLoginBaseDiscoveryListener = this;
            initiateDiscovery();
        } else {
            refreshXmlRpcSites();
        }
    }

    private void refreshXmlRpcSites() {
        RefreshSitesXMLRPCPayload selfHostedPayload = new RefreshSitesXMLRPCPayload();
        selfHostedPayload.username = mRequestedUsername;
        selfHostedPayload.password = mRequestedPassword;
        selfHostedPayload.url = mEndpointAddress;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(selfHostedPayload));
    }

    private String getCleanedUsername() {
        return EditTextUtils.getText(mUsernameInput.getEditText()).trim();
    }

    @Override
    public void onEditorCommit() {
        showError(null);
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
        showError(null);
        updatePrimaryButtonEnabledStatus();
    }

    @Override
    @NonNull public String getRequestedSiteAddress() {
        return mInputSiteAddressWithoutSuffix;
    }

    /**
     * Woo users:
     * [HTTP_AUTH_REQUIRED] is not supported by Jetpack and can only occur if jetpack is not
     * available. Redirect to Jetpack required screen.
     *
     * The other discovery errors can take place even if Jetpack is available.
     * Furthermore, for errors such as [MISSING_XMLRPC_METHOD], [XMLRPC_BLOCKED], [XMLRPC_FORBIDDEN]
     * [NO_SITE_ERROR] and [GENERIC_ERROR], the jetpack available flag from the CONNECT_SITE_INFO
     * API returns false even if Jetpack is available for the site.
     * So we redirect to discovery error screen without checking for Jetpack availability.
     * */
    @Override
    public void handleDiscoveryError(DiscoveryError error, String failedEndpoint) {
        ActivityUtils.hideKeyboard(getActivity());
        mAnalyticsListener.trackFailure(error.name() + " - " + failedEndpoint);
        if (error == DiscoveryError.HTTP_AUTH_REQUIRED) {
            mLoginListener.helpNoJetpackScreen(mInputSiteAddress, mEndpointAddress,
                    getCleanedUsername(), mPasswordInput.getEditText().getText().toString(),
                    mAccountStore.getAccount().getAvatarUrl(), true);
        } else {
            mLoginListener.helpHandleDiscoveryError(mInputSiteAddress, mEndpointAddress,
                    getCleanedUsername(), mPasswordInput.getEditText().getText().toString(),
                    mAccountStore.getAccount().getAvatarUrl(), getDiscoveryErrorMessage(error));
        }
    }

    private int getDiscoveryErrorMessage(DiscoveryError error) {
        int errorMessageId = 0;
        switch (error) {
            case HTTP_AUTH_REQUIRED:
                errorMessageId = R.string.login_discovery_error_http_auth;
                break;
            case ERRONEOUS_SSL_CERTIFICATE:
                errorMessageId = R.string.login_discovery_error_ssl;
                break;
            case INVALID_URL:
            case NO_SITE_ERROR:
            case WORDPRESS_COM_SITE:
            case GENERIC_ERROR:
                errorMessageId = R.string.login_discovery_error_generic;
                break;

            case MISSING_XMLRPC_METHOD:
            case XMLRPC_BLOCKED:
            case XMLRPC_FORBIDDEN:
                errorMessageId = R.string.login_discovery_error_xmlrpc;
                break;
        }
        return errorMessageId;
    }

    @Override
    public void handleWpComDiscoveryError(String failedEndpoint) {
        AppLog.e(T.API, "Inputted a wpcom address in site address screen. Redirecting to Email screen");
        mLoginListener.gotWpcomSiteInfo(UrlUtils.removeScheme(failedEndpoint));
    }

    @Override
    public void handleDiscoverySuccess(@NonNull String endpointAddress) {
        mEndpointAddress = endpointAddress;
        refreshXmlRpcSites();
    }

    private void showUsernameError(String errorMessage) {
        mAnalyticsListener.trackFailure(errorMessage);
        mUsernameInput.setError(errorMessage);
        mPasswordInput.setError(null);

        if (errorMessage != null) {
            requestScrollToView(mUsernameInput);
        }
    }

    private void showPasswordError(String errorMessage) {
        mUsernameInput.setError(null);
        mAnalyticsListener.trackFailure(errorMessage);
        mPasswordInput.setError(errorMessage);

        if (errorMessage != null) {
            requestScrollToView(mPasswordInput);
        }
    }

    private void showError(String errorMessage) {
        mUsernameInput.setError(errorMessage != null ? " " : null);
        mPasswordInput.setError(errorMessage);
        mAnalyticsListener.trackFailure(errorMessage);

        if (errorMessage != null) {
            requestScrollToView(mPasswordInput);
        }
    }

    private void requestScrollToView(final View view) {
        view.post(new Runnable() {
            @Override
            public void run() {
                Rect rect = new Rect(); // Coordinates to scroll to
                view.getHitRect(rect);
                mScrollView.requestChildRectangleOnScreen(view, rect, false);
            }
        });
    }

    private @Nullable SiteModel detectNewlyAddedXMLRPCSite() {
        List<SiteModel> selfhostedSites = mSiteStore.getSitesAccessedViaXMLRPC();
        for (SiteModel site : selfhostedSites) {
            if (!mOldSitesIDs.contains(site.getId())) {
                return site;
            }
        }

        return null;
    }

    @Override
    protected void endProgress() {
        super.endProgress();
        mRequestedUsername = null;
        mRequestedPassword = null;
    }

    private void handleAuthError(AuthenticationErrorType error, String errorMessage) {
        switch (error) {
            case INCORRECT_USERNAME_OR_PASSWORD:
            case NOT_AUTHENTICATED: // NOT_AUTHENTICATED is the generic error from XMLRPC response on first call.
            case HTTP_AUTH_ERROR:
                showError(getString(R.string.username_or_password_incorrect));
                break;
            case INVALID_OTP:
            case INVALID_TOKEN:
            case AUTHORIZATION_REQUIRED:
            case NEEDS_2FA:
                if (mIsWpcom) {
                    if (mLoginListener != null) {
                        mLoginListener.needs2fa(mRequestedUsername, mRequestedPassword);
                    }
                } else {
                    showError("2FA not supported for self-hosted sites. Please use an app-password.");
                }
                break;
            default:
                AppLog.e(T.NUX, "Server response: " + errorMessage);

                ToastUtils.showToast(getActivity(),
                        TextUtils.isEmpty(errorMessage) ? getString(R.string.error_generic) : errorMessage);
                break;
        }
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        // emitted when wpcom site or when the selfhosted login failed (but not when succeeded)

        if (!isAdded() || mLoginFinished) {
            // just bail
            return;
        }

        if (event.isError()) {
            mLoginStarted = false;
            if (mRequestedUsername == null) {
                // just bail since the operation was cancelled
                return;
            }

            mAuthFailed = true;
            AppLog.e(T.API, "Login with username/pass onAuthenticationChanged has error: " + event.error.type
                    + " - " + event.error.message);
            mAnalyticsListener.trackLoginFailed(event.getClass().getSimpleName(),
                    event.error.type.toString(), event.error.message);

            handleAuthError(event.error.type, event.error.message);

            // end the progress last since it cleans up the requested username/password and those might be needed
            //  in handleAuthError()
            endProgress();

            return;
        }

        AppLog.i(T.NUX, "onAuthenticationChanged: " + event.toString());

        doFinishLogin();
    }

    @Override
    protected void onLoginFinished() {
        mAnalyticsListener.trackAnalyticsSignIn(mIsWpcom);

        mLoginListener.startPostLoginServices();

        mLoginListener.loggedInViaPassword(mOldSitesIDs);
    }

    private void finishLogin() {
        mAnalyticsListener.trackAnalyticsSignIn(mIsWpcom);

        // mark as finished so any subsequent onSiteChanged (e.g. triggered by WPMainActivity) won't be intercepted
        mLoginFinished = true;

        if (mLoginListener != null) {
            if (mIsWpcom) {
                saveCredentialsInSmartLock(mLoginListener, mRequestedUsername, mRequestedPassword);
            }

            mLoginListener.loggedInViaUsernamePassword(mOldSitesIDs);
        }

        endProgress();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        if (!isAdded() || mLoginFinished || !mLoginStarted) {
            return;
        }

        if (event.isError()) {
            mLoginStarted = false;
            if (mRequestedUsername == null) {
                // just bail since the operation was cancelled
                return;
            }

            endProgress();

            if (mLoginListener.getLoginMode() == LoginMode.WOO_LOGIN_MODE) {
                // Woo users: One of the errors that can happen here is the XML-RPC endpoint could
                // be blocked by plugins such as `Disable XML-RPC`. Redirect the user to discovery
                // error screen in such cases.
                handleDiscoveryError(DiscoveryError.XMLRPC_BLOCKED, mInputSiteAddress);
                return;
            }

            String errorMessage;
            if (event.error.type == SiteErrorType.DUPLICATE_SITE) {
                if (event.rowsAffected == 0) {
                    // If there is a duplicate site and not any site has been added, show an error and
                    // stop the sign in process
                    errorMessage = getString(R.string.cannot_add_duplicate_site);
                } else {
                    // If there is a duplicate site, notify the user something could be wrong,
                    // but continue the sign in process
                    errorMessage = getString(R.string.duplicate_site_detected);
                }
            } else {
                errorMessage = getString(R.string.login_error_while_adding_site, event.error.type.toString());
            }

            AppLog.e(T.API, "Login with username/pass onSiteChanged has error: " + event.error.type
                    + " - " + errorMessage);

            if (!mAuthFailed) {
                // show the error if not already displayed in onAuthenticationChanged (like in username/pass error)
                showError(errorMessage);
            }

            return;
        }

        if (!mIsWpcom && mLoginListener.getLoginMode() == LoginMode.WOO_LOGIN_MODE) {
            SiteModel lastAddedXMLRPCSite = SiteUtils.getXMLRPCSiteByUrl(mSiteStore, mInputSiteAddress);
            if (lastAddedXMLRPCSite != null) {
                // the wp.getOptions endpoint is already called
                // verify if jetpack user email is available.
                // If not, redirect to jetpack required screen. Otherwise, initiate magic sign in
                if (mGetSiteOptionsInitiated) {
                    endProgress();
                    mGetSiteOptionsInitiated = false;
                    String userEmail = lastAddedXMLRPCSite.getJetpackUserEmail();
                    ActivityUtils.hideKeyboard(getActivity());
                    if (userEmail == null || userEmail.isEmpty()) {
                        mLoginListener.helpNoJetpackScreen(lastAddedXMLRPCSite.getUrl(),
                                lastAddedXMLRPCSite.getXmlRpcUrl(), lastAddedXMLRPCSite.getUsername(),
                                lastAddedXMLRPCSite.getPassword(), mAccountStore.getAccount().getAvatarUrl(),
                                false);
                    } else {
                        mLoginListener.gotWpcomEmail(userEmail, true, null);
                    }
                } else {
                    // Initiate the wp.getOptions endpoint to fetch the jetpack user email
                    mGetSiteOptionsInitiated = true;
                    mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(lastAddedXMLRPCSite));
                }
            }
            return;
        }

        SiteModel newlyAddedXMLRPCSite = detectNewlyAddedXMLRPCSite();
        // newlyAddedSite will be null if the user sign in with wpcom credentials
        if (newlyAddedXMLRPCSite != null && !newlyAddedXMLRPCSite.isUsingWpComRestApi()) {
            mDispatcher.dispatch(SiteActionBuilder.newFetchProfileXmlRpcAction(newlyAddedXMLRPCSite));
        } else {
            finishLogin();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onProfileFetched(OnProfileFetched event) {
        if (!isAdded() || mLoginFinished) {
            return;
        }

        if (event.isError()) {
            if (mRequestedUsername == null) {
                // just bail since the operation was cancelled
                return;
            }

            endProgress();

            AppLog.e(T.API, "Fetching selfhosted site profile has error: " + event.error.type + " - "
                    + event.error.message);

            // continue with success, even if the operation was cancelled since the user got logged in regardless.
            // So, go on with finishing the login process
        }
        finishLogin();
    }
}

