package org.wordpress.android.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
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
import androidx.lifecycle.Observer;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload;
import org.wordpress.android.fluxc.store.SiteStore.OnConnectSiteInfoChecked;
import org.wordpress.android.login.util.SiteUtils;
import org.wordpress.android.login.widgets.WPLoginInputRow;
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class LoginSiteAddressFragment extends LoginBaseDiscoveryFragment implements TextWatcher,
        OnEditorCommitListener, LoginBaseDiscoveryFragment.LoginBaseDiscoveryListener {
    private static final String KEY_REQUESTED_SITE_ADDRESS = "KEY_REQUESTED_SITE_ADDRESS";

    private static final String KEY_SITE_INFO_URL = "url";
    private static final String KEY_SITE_INFO_URL_AFTER_REDIRECTS = "url_after_redirects";
    private static final String KEY_SITE_INFO_EXISTS = "exists";
    private static final String KEY_SITE_INFO_HAS_JETPACK = "has_jetpack";
    private static final String KEY_SITE_INFO_IS_JETPACK_ACTIVE = "is_jetpack_active";
    private static final String KEY_SITE_INFO_IS_JETPACK_CONNECTED = "is_jetpack_connected";
    private static final String KEY_SITE_INFO_IS_WORDPRESS = "is_wordpress";
    private static final String KEY_SITE_INFO_IS_WPCOM = "is_wp_com";
    private static final String KEY_SITE_INFO_CALCULATED_HAS_JETPACK = "login_calculated_has_jetpack";

    public static final String TAG = "login_site_address_fragment_tag";

    private WPLoginInputRow mSiteAddressInput;

    private String mRequestedSiteAddress;

    private String mConnectSiteInfoUrl;
    private String mConnectSiteInfoUrlRedirect;
    private boolean mConnectSiteInfoCalculatedHasJetpack;

    private LoginSiteAddressValidator mLoginSiteAddressValidator;

    @Inject AccountStore mAccountStore;
    @Inject Dispatcher mDispatcher;
    @Inject HTTPAuthManager mHTTPAuthManager;
    @Inject MemorizingTrustManager mMemorizingTrustManager;

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.login_site_address_screen;
    }

    @Override
    protected @LayoutRes int getProgressBarText() {
        return R.string.login_checking_site_address;
    }

    @Override
    protected void setupLabel(@NonNull TextView label) {
        if (mLoginListener.getLoginMode() == LoginMode.SHARE_INTENT) {
            label.setText(R.string.enter_site_address_share_intent);
        } else {
            label.setText(R.string.enter_site_address);
        }
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        // important for accessibility - talkback
        getActivity().setTitle(R.string.site_address_login_title);
        mSiteAddressInput = rootView.findViewById(R.id.login_site_address_row);
        if (BuildConfig.DEBUG) {
            mSiteAddressInput.getEditText().setText(BuildConfig.DEBUG_WPCOM_WEBSITE_URL);
        }
        mSiteAddressInput.addTextChangedListener(this);
        mSiteAddressInput.setOnEditorCommitListener(this);

        rootView.findViewById(R.id.login_site_address_help_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mAnalyticsListener.trackShowHelpClick();
                showSiteAddressHelp();
            }
        });
    }

    @Override
    protected void setupBottomButton(Button button) {
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                discover();
            }
        });
    }

    @Override
    protected void buildToolbar(Toolbar toolbar, ActionBar actionBar) {
        actionBar.setTitle(R.string.log_in);
    }

    @Override
    protected EditText getEditTextToFocusOnStart() {
        return mSiteAddressInput.getEditText();
    }

    @Override
    protected void onHelp() {
        if (mLoginListener != null) {
            mLoginListener.helpSiteAddress(mRequestedSiteAddress);
        }
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mRequestedSiteAddress = savedInstanceState.getString(KEY_REQUESTED_SITE_ADDRESS);
            mConnectSiteInfoUrl = savedInstanceState.getString(KEY_SITE_INFO_URL);
            mConnectSiteInfoUrlRedirect =
                    savedInstanceState.getString(KEY_SITE_INFO_URL_AFTER_REDIRECTS);
            mConnectSiteInfoCalculatedHasJetpack =
                    savedInstanceState.getBoolean(KEY_SITE_INFO_CALCULATED_HAS_JETPACK);
        } else {
            mAnalyticsListener.trackUrlFormViewed();
        }

        mLoginSiteAddressValidator = new LoginSiteAddressValidator();

        mLoginSiteAddressValidator.getIsValid().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override public void onChanged(Boolean enabled) {
                getBottomButton().setEnabled(enabled);
            }
        });
        mLoginSiteAddressValidator.getErrorMessageResId().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override public void onChanged(Integer resId) {
                if (resId != null) {
                    showError(resId);
                } else {
                    mSiteAddressInput.setError(null);
                }
            }
        });
    }

    @Override public void onResume() {
        super.onResume();

        mAnalyticsListener.siteAddressFormScreenResumed();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_REQUESTED_SITE_ADDRESS, mRequestedSiteAddress);
        outState.putString(KEY_SITE_INFO_URL, mConnectSiteInfoUrl);
        outState.putString(KEY_SITE_INFO_URL_AFTER_REDIRECTS, mConnectSiteInfoUrlRedirect);
        outState.putBoolean(KEY_SITE_INFO_CALCULATED_HAS_JETPACK,
                mConnectSiteInfoCalculatedHasJetpack);
    }

    @Override public void onDestroyView() {
        mLoginSiteAddressValidator.dispose();
        mSiteAddressInput = null;

        super.onDestroyView();
    }

    protected void discover() {
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }
        mAnalyticsListener.trackSubmitClicked();

        mLoginBaseDiscoveryListener = this;

        mRequestedSiteAddress = mLoginSiteAddressValidator.getCleanedSiteAddress();

        String cleanedXmlrpcSuffix = UrlUtils.removeXmlrpcSuffix(mRequestedSiteAddress);

        mAnalyticsListener.trackConnectedSiteInfoRequested(cleanedXmlrpcSuffix);
        mDispatcher.dispatch(SiteActionBuilder.newFetchConnectSiteInfoAction(cleanedXmlrpcSuffix));

        startProgress();
    }

    @Override
    public void onEditorCommit() {
        if (getBottomButton().isEnabled()) {
            discover();
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (mSiteAddressInput != null) {
            mLoginSiteAddressValidator
                    .setAddress(EditTextUtils.getText(mSiteAddressInput.getEditText()));
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        mConnectSiteInfoUrl = null;
        mConnectSiteInfoUrlRedirect = null;
        mConnectSiteInfoCalculatedHasJetpack = false;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mSiteAddressInput != null) {
            mSiteAddressInput.setError(null);
        }
    }

    private void showError(int messageId) {
        String message = getString(messageId);
        mAnalyticsListener.trackFailure(message);
        mSiteAddressInput.setError(message);
    }

    @Override
    protected void endProgress() {
        super.endProgress();
        mRequestedSiteAddress = null;
    }

    @Override
    @NonNull public String getRequestedSiteAddress() {
        return mRequestedSiteAddress;
    }

    @Override
    public void handleDiscoveryError(DiscoveryError error, final String failedEndpoint) {
        switch (error) {
            case ERRONEOUS_SSL_CERTIFICATE:
                mLoginListener.handleSslCertificateError(mMemorizingTrustManager,
                        new LoginListener.SelfSignedSSLCallback() {
                            @Override
                            public void certificateTrusted() {
                                if (failedEndpoint == null) {
                                    return;
                                }
                                // retry site lookup
                                discover();
                            }
                        });
                break;
            case HTTP_AUTH_REQUIRED:
                askForHttpAuthCredentials(failedEndpoint);
                break;
            case NO_SITE_ERROR:
                showError(R.string.no_site_error);
                break;
            case INVALID_URL:
                showError(R.string.invalid_site_url_message);
                mAnalyticsListener.trackInsertedInvalidUrl();
                break;
            case MISSING_XMLRPC_METHOD:
                showError(R.string.xmlrpc_missing_method_error);
                break;
            case WORDPRESS_COM_SITE:
                // This is handled by handleWpComDiscoveryError
                break;
            case XMLRPC_BLOCKED:
                showError(R.string.xmlrpc_post_blocked_error);
                break;
            case XMLRPC_FORBIDDEN:
                showError(R.string.xmlrpc_endpoint_forbidden_error);
                break;
            case GENERIC_ERROR:
                showError(R.string.error_generic);
                break;
        }
    }

    @Override
    public void handleWpComDiscoveryError(String failedEndpoint) {
        AppLog.e(T.API, "Inputted a wpcom address in site address screen.");

        // If the user is already logged in a wordpress.com account, bail out
        if (mAccountStore.hasAccessToken()) {
            String currentUsername = mAccountStore.getAccount().getUserName();
            AppLog.e(T.NUX, "User is already logged in WordPress.com: " + currentUsername);

            ArrayList<Integer> oldSitesIDs = SiteUtils.getCurrentSiteIds(mSiteStore, true);
            mLoginListener.alreadyLoggedInWpcom(oldSitesIDs);
        } else {
            mLoginListener.gotWpcomSiteInfo(failedEndpoint);
        }
    }

    @Override
    public void handleDiscoverySuccess(@NonNull String endpointAddress) {
        AppLog.i(T.NUX, "Discovery succeeded, endpoint: " + endpointAddress);

        // hold the URL in a variable to use below otherwise it gets cleared up by endProgress
        String inputSiteAddress = mRequestedSiteAddress;
        endProgress();
        if (mLoginListener.getLoginMode() == LoginMode.WOO_LOGIN_MODE) {
            mLoginListener.gotConnectedSiteInfo(
                    mConnectSiteInfoUrl,
                    mConnectSiteInfoUrlRedirect,
                    mConnectSiteInfoCalculatedHasJetpack
                                               );
        } else {
            mLoginListener.gotXmlRpcEndpoint(inputSiteAddress, endpointAddress);
        }
    }

    private void askForHttpAuthCredentials(@NonNull final String url) {
        LoginHttpAuthDialogFragment loginHttpAuthDialogFragment = LoginHttpAuthDialogFragment.newInstance(url);
        loginHttpAuthDialogFragment.setTargetFragment(this, LoginHttpAuthDialogFragment.DO_HTTP_AUTH);
        loginHttpAuthDialogFragment.show(getFragmentManager(), LoginHttpAuthDialogFragment.TAG);
    }

    private void showSiteAddressHelp() {
        new LoginSiteAddressHelpDialogFragment().show(getFragmentManager(), LoginSiteAddressHelpDialogFragment.TAG);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LoginHttpAuthDialogFragment.DO_HTTP_AUTH && resultCode == Activity.RESULT_OK) {
            String url = data.getStringExtra(LoginHttpAuthDialogFragment.ARG_URL);
            String httpUsername = data.getStringExtra(LoginHttpAuthDialogFragment.ARG_USERNAME);
            String httpPassword = data.getStringExtra(LoginHttpAuthDialogFragment.ARG_PASSWORD);
            mHTTPAuthManager.addHTTPAuthCredentials(httpUsername, httpPassword, url, null);
            discover();
        }
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFetchedConnectSiteInfo(OnConnectSiteInfoChecked event) {
        if (mRequestedSiteAddress == null) {
            // bail if user canceled
            return;
        }

        if (!isAdded()) {
            return;
        }

        if (event.isError()) {
            mAnalyticsListener.trackConnectedSiteInfoFailed(
                    mRequestedSiteAddress,
                    event.getClass().getSimpleName(),
                    event.error.type.name(),
                    event.error.message);

            AppLog.e(T.API, "onFetchedConnectSiteInfo has error: " + event.error.message);
            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                showError(R.string.invalid_site_url_message);
            } else {
                showError(R.string.error_generic_network);
            }

            endProgressIfNeeded();
        } else {
            boolean hasJetpack = calculateHasJetpack(event.info);

            mConnectSiteInfoUrl = event.info.url;
            mConnectSiteInfoUrlRedirect = event.info.urlAfterRedirects;
            mConnectSiteInfoCalculatedHasJetpack = hasJetpack;

            mAnalyticsListener.trackConnectedSiteInfoSucceeded(createConnectSiteInfoProperties(event.info, hasJetpack));

            if (mLoginListener.getLoginMode() == LoginMode.WOO_LOGIN_MODE) {
                handleConnectSiteInfoForWoo(event.info);
            } else if (mLoginListener.getLoginMode() == LoginMode.JETPACK_LOGIN_ONLY) {
                handleConnectSiteInfoForJetpack(event.info);
            } else {
                handleConnectSiteInfoForWordPress(event.info);
            }
        }
    }

    private void handleConnectSiteInfoForWoo(ConnectSiteInfoPayload siteInfo) {
        if (!siteInfo.exists) {
            endProgressIfNeeded();
            // Site does not exist
            showError(R.string.invalid_site_url_message);
        } else if (!siteInfo.isWordPress) {
            endProgressIfNeeded();
            // Not a WordPress site
            mLoginListener.handleSiteAddressError(siteInfo);
        } else if (siteInfo.hasJetpack && siteInfo.isJetpackConnected && siteInfo.isJetpackActive) {
            endProgressIfNeeded();
            mLoginListener.gotConnectedSiteInfo(
                    mConnectSiteInfoUrl,
                    mConnectSiteInfoUrlRedirect,
                    mConnectSiteInfoCalculatedHasJetpack
            );
        } else {
            /**
             * Jetpack internally uses xml-rpc protocol. Due to a bug on the API, when jetpack is
             * setup and connected to a .com account `isJetpackConnected` returns false when xml-rpc
             * is disabled.
             * This is causing issues to the client apps as they can't differentiate between
             * "xml-rpc disabled" and "jetpack not connected" states. Therefore, the login flow
             * library needs to invoke "xml-rpc discovery" to check if xml-rpc is accessible.
             */
            initiateDiscovery();
        }
    }

    private void handleConnectSiteInfoForWordPress(ConnectSiteInfoPayload siteInfo) {
        if (siteInfo.isWPCom) {
            // It's a Simple or Atomic site
            if (mLoginListener.getLoginMode() == LoginMode.SELFHOSTED_ONLY) {
                // We're only interested in self-hosted sites
                if (siteInfo.hasJetpack) {
                    // This is an Atomic site, so treat it as self-hosted and start the discovery process
                    initiateDiscovery();
                    return;
                }
            }
            endProgressIfNeeded();
            mLoginListener.gotWpcomSiteInfo(UrlUtils.removeScheme(siteInfo.url));
        } else {
            // It's a Jetpack or self-hosted site
            if (mLoginListener.getLoginMode() == LoginMode.WPCOM_LOGIN_ONLY) {
                // We're only interested in WordPress.com accounts
                showError(R.string.enter_wpcom_or_jetpack_site);
                endProgressIfNeeded();
            } else {
                // Start the discovery process
                initiateDiscovery();
            }
        }
    }

    private void handleConnectSiteInfoForJetpack(ConnectSiteInfoPayload siteInfo) {
        endProgressIfNeeded();

        if (siteInfo.hasJetpack && siteInfo.isJetpackConnected && siteInfo.isJetpackActive) {
            mLoginListener.gotWpcomSiteInfo(UrlUtils.removeScheme(siteInfo.url));
        } else {
            mLoginListener.handleSiteAddressError(siteInfo);
        }
    }

    private boolean calculateHasJetpack(ConnectSiteInfoPayload siteInfo) {
        // Determining if jetpack is actually installed takes additional logic. This final
        // calculated event property will make querying this event more straight-forward.
        // Internal reference: p99K0U-1vO-p2#comment-3574
        boolean hasJetpack = false;
        if (siteInfo.isWPCom && siteInfo.hasJetpack) {
            // This is likely an atomic site.
            hasJetpack = true;
        } else if (siteInfo.isJetpackConnected) {
            hasJetpack = true;
        }
        return hasJetpack;
    }

    private Map<String, String> createConnectSiteInfoProperties(ConnectSiteInfoPayload siteInfo, boolean hasJetpack) {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(KEY_SITE_INFO_URL, siteInfo.url);
        properties.put(KEY_SITE_INFO_URL_AFTER_REDIRECTS, siteInfo.urlAfterRedirects);
        properties.put(KEY_SITE_INFO_EXISTS, Boolean.toString(siteInfo.exists));
        properties.put(KEY_SITE_INFO_HAS_JETPACK, Boolean.toString(siteInfo.hasJetpack));
        properties.put(KEY_SITE_INFO_IS_JETPACK_ACTIVE, Boolean.toString(siteInfo.isJetpackActive));
        properties.put(KEY_SITE_INFO_IS_JETPACK_CONNECTED, Boolean.toString(siteInfo.isJetpackConnected));
        properties.put(KEY_SITE_INFO_IS_WORDPRESS, Boolean.toString(siteInfo.isWordPress));
        properties.put(KEY_SITE_INFO_IS_WPCOM, Boolean.toString(siteInfo.isWPCom));
        properties.put(KEY_SITE_INFO_CALCULATED_HAS_JETPACK, Boolean.toString(hasJetpack));
        return properties;
    }
}
