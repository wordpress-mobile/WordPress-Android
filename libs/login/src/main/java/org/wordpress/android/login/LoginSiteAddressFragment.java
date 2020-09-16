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
import org.wordpress.android.fluxc.store.SiteStore.OnConnectSiteInfoChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnWPComSiteFetched;
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

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class LoginSiteAddressFragment extends LoginBaseDiscoveryFragment implements TextWatcher,
        OnEditorCommitListener, LoginBaseDiscoveryFragment.LoginBaseDiscoveryListener {
    private static final String KEY_REQUESTED_SITE_ADDRESS = "KEY_REQUESTED_SITE_ADDRESS";

    public static final String TAG = "login_site_address_fragment_tag";

    private WPLoginInputRow mSiteAddressInput;

    private String mRequestedSiteAddress;

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
        switch (mLoginListener.getLoginMode()) {
            case SHARE_INTENT:
                label.setText(R.string.enter_site_address_share_intent);
                break;
            default:
                label.setText(R.string.enter_site_address);
                break;
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
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        secondaryButton.setVisibility(View.GONE);
        primaryButton.setOnClickListener(new OnClickListener() {
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
        } else {
            mAnalyticsListener.trackUrlFormViewed();
        }

        mLoginSiteAddressValidator = new LoginSiteAddressValidator();

        mLoginSiteAddressValidator.getIsValid().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override public void onChanged(Boolean enabled) {
                getPrimaryButton().setEnabled(enabled);
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_REQUESTED_SITE_ADDRESS, mRequestedSiteAddress);
    }

    @Override public void onDestroyView() {
        mLoginSiteAddressValidator.dispose();

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

        if (mLoginListener.getLoginMode() == LoginMode.WOO_LOGIN_MODE) {
            mAnalyticsListener.trackConnectedSiteInfoRequested(cleanedXmlrpcSuffix);
            mDispatcher.dispatch(SiteActionBuilder.newFetchConnectSiteInfoAction(cleanedXmlrpcSuffix));
        } else {
            mDispatcher.dispatch(SiteActionBuilder.newFetchWpcomSiteByUrlAction(cleanedXmlrpcSuffix));
        }

        startProgress();
    }

    @Override
    public void onEditorCommit() {
        if (getPrimaryButton().isEnabled()) {
            discover();
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        mLoginSiteAddressValidator.setAddress(EditTextUtils.getText(mSiteAddressInput.getEditText()));
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mSiteAddressInput.setError(null);
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
            mLoginListener.gotWpcomSiteInfo(failedEndpoint, null, null);
        }
    }

    @Override
    public void handleDiscoverySuccess(@NonNull String endpointAddress) {
        AppLog.i(T.NUX, "Discovery succeeded, endpoint: " + endpointAddress);

        // hold the URL in a variable to use below otherwise it gets cleared up by endProgress
        String inputSiteAddress = mRequestedSiteAddress;
        endProgress();
        mLoginListener.gotXmlRpcEndpoint(inputSiteAddress, endpointAddress);
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
    public void onWPComSiteFetched(OnWPComSiteFetched event) {
        if (mRequestedSiteAddress == null) {
            // bail if user canceled
            return;
        }

        if (!isAdded()) {
            return;
        }

        if (event.isError()) {
            // Not a WordPress.com or Jetpack site
            if (mLoginListener.getLoginMode() == LoginMode.WPCOM_LOGIN_ONLY) {
                showError(R.string.enter_wpcom_or_jetpack_site);
                endProgress();
            } else {
                // Start the discovery process
                initiateDiscovery();
            }
        } else {
            if (event.site.isJetpackInstalled() && mLoginListener.getLoginMode() != LoginMode.WPCOM_LOGIN_ONLY) {
                // If Jetpack site, treat it as self-hosted and start the discovery process
                // An exception is WPCOM_LOGIN_ONLY mode - in that case we're only interested in adding sites
                // through WordPress.com login, and should proceed along that login path
                initiateDiscovery();
                return;
            }

            endProgress();

            // it's a wp.com site so, treat it as such.
            mLoginListener.gotWpcomSiteInfo(
                    UrlUtils.removeScheme(event.site.getUrl()),
                    event.site.getName(),
                    event.site.getIconUrl());
        }
    }

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

        // hold the URL in a variable to use below otherwise it gets cleared up by endProgress
        final String requestedSiteAddress = mRequestedSiteAddress;

        if (isInProgress()) {
            endProgress();
        }

        if (event.isError()) {
            mAnalyticsListener.trackConnectedSiteInfoFailed(
                    requestedSiteAddress,
                    event.getClass().getSimpleName(),
                    event.error.type.name(),
                    event.error.message);

            AppLog.e(T.API, "onFetchedConnectSiteInfo has error: " + event.error.message);

            showError(R.string.invalid_site_url_message);
        } else {
            // TODO: If we plan to keep this logic we should convert these labels to constants
            HashMap<String, String> properties = new HashMap<>();
            properties.put("url", event.info.url);
            properties.put("url_after_redirects", event.info.urlAfterRedirects);
            properties.put("exists", Boolean.toString(event.info.exists));
            properties.put("has_jetpack", Boolean.toString(event.info.hasJetpack));
            properties.put("is_jetpack_active", Boolean.toString(event.info.isJetpackActive));
            properties.put("is_jetpack_connected", Boolean.toString(event.info.isJetpackConnected));
            properties.put("is_wordpress", Boolean.toString(event.info.isWordPress));
            properties.put("is_wp_com", Boolean.toString(event.info.isWPCom));

            // Determining if jetpack is actually installed takes additional logic. This final
            // calculated event property will make querying this event more straight-forward:
            boolean hasJetpack = false;
            if (event.info.isWPCom && event.info.hasJetpack) {
                // This is likely an atomic site.
                hasJetpack = true;
            } else if (event.info.isJetpackConnected) {
                hasJetpack = true;
            }
            properties.put("login_calculated_has_jetpack", Boolean.toString(hasJetpack));
            mAnalyticsListener.trackConnectedSiteInfoSucceeded(properties);

            if (!event.info.exists) {
                // Site does not exist
                showError(R.string.invalid_site_url_message);
            } else if (!event.info.isWordPress) {
                // Not a WordPress site
                showError(R.string.enter_wordpress_site);
            } else {
                mLoginListener.gotConnectedSiteInfo(
                        event.info.url,
                        event.info.urlAfterRedirects,
                        hasJetpack);
            }
        }
    }
}
