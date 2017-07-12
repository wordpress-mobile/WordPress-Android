package org.wordpress.android.ui.accounts.login;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SelfSignedSSLUtils;
import org.wordpress.android.util.SelfSignedSSLUtils.Callback;
import org.wordpress.android.util.UrlUtils;

import javax.inject.Inject;

public class LoginSiteAddressFragment extends LoginBaseFormFragment implements TextWatcher {
    private static final String KEY_REQUESTED_SITE_ADDRESS = "KEY_REQUESTED_SITE_ADDRESS";

    public static final String TAG = "login_email_fragment_tag";

    private static final String XMLRPC_BLOCKED_HELPSHIFT_FAQ_SECTION = "10";
    private static final String XMLRPC_BLOCKED_HELPSHIFT_FAQ_ID = "102";

    private static final String MISSING_XMLRPC_METHOD_HELPSHIFT_FAQ_SECTION = "10";
    private static final String MISSING_XMLRPC_METHOD_HELPSHIFT_FAQ_ID = "11";

    private static final String NO_SITE_HELPSHIFT_FAQ_SECTION = "10";
    private static final String NO_SITE_HELPSHIFT_FAQ_ID = "2"; //using the same as in INVALID URL

    private TextInputLayout mSiteAddressTextLayout;
    private EditText mSiteAddressEditText;

    private String mRequestedSiteAddress;

    @Inject AccountStore mAccountStore;
    @Inject Dispatcher mDispatcher;
    @Inject HTTPAuthManager mHTTPAuthManager;
    @Inject MemorizingTrustManager mMemorizingTrustManager;

    @Override
    protected @LayoutRes
    int getContentLayout() {
        return R.layout.login_site_address_screen;
    }

    @Override
    protected @LayoutRes int getProgressBarText() {
        return R.string.login_checking_site_address;
    }

    @Override
    protected void setupLabel(TextView label) {
        label.setText(R.string.enter_site_address);
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        mSiteAddressTextLayout = (TextInputLayout) rootView.findViewById(R.id.input_layout);
        mSiteAddressTextLayout.setHint(getString(R.string.login_site_address));

        ImageView icon = (ImageView) rootView.findViewById(R.id.icon);
        icon.setContentDescription(getString(R.string.login_globe_icon));
        icon.setImageResource(R.drawable.ic_globe_grey_24dp);

        mSiteAddressEditText = (EditText) rootView.findViewById(R.id.input);
        mSiteAddressEditText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        mSiteAddressEditText.addTextChangedListener(this);

        mSiteAddressEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null
                        && event.getAction() == KeyEvent.ACTION_UP
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    discover();
                }

                // always consume the event so the focus stays in the EditText
                return true;
            }
        });
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        secondaryButton.setText(R.string.login_site_address_help);
        secondaryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showSiteAddressHelp();
            }
        });
        primaryButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                discover();
            }
        });
    }

    @Override
    protected EditText getEditTextToFocusOnStart() {
        return mSiteAddressEditText;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mRequestedSiteAddress = savedInstanceState.getString(KEY_REQUESTED_SITE_ADDRESS);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_REQUESTED_SITE_ADDRESS, mRequestedSiteAddress);
    }

    protected void discover() {
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        if (TextUtils.isEmpty(mSiteAddressEditText.getText())) {
            showError(R.string.login_empty_site_url, null, null);
            return;
        }

        mRequestedSiteAddress = getCleanedSiteAddress();

        Uri uri = Uri.parse(UrlUtils.addUrlSchemeIfNeeded(mRequestedSiteAddress, false));
        mDispatcher.dispatch(SiteActionBuilder.newFetchWpcomSiteByUrlAction(uri.getHost()));

        showProgressDialog();
    }

    private String getCleanedSiteAddress() {
        return EditTextUtils.getText(mSiteAddressEditText).trim().replaceAll("[\r\n]", "");
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mSiteAddressTextLayout.setError(null);
        mLoginListener.setHelpContext(null, null);
    }

    private void showError(int messageId, String faqId, String faqSection) {
        mSiteAddressTextLayout.setError(getString(messageId));
        mLoginListener.setHelpContext(faqId, faqSection);
    }

    @Override
    protected void endProgress() {
        super.endProgress();
        mRequestedSiteAddress = null;
    }

    private void askForHttpAuthCredentials(@NonNull final String url) {
        LoginHttpAuthDialogFragment loginHttpAuthDialogFragment = LoginHttpAuthDialogFragment.newInstance(url);
        loginHttpAuthDialogFragment.setTargetFragment(this, LoginHttpAuthDialogFragment.DO_HTTP_AUTH);
        loginHttpAuthDialogFragment.show(getFragmentManager(), LoginHttpAuthDialogFragment.TAG);
    }

    public void handleDiscoveryError(DiscoveryError error, final String failedEndpoint) {
        switch (error) {
            case ERRONEOUS_SSL_CERTIFICATE:
                SelfSignedSSLUtils.showSSLWarningDialog(getActivity(), mMemorizingTrustManager,
                        new Callback() {
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
                showError(R.string.no_site_error, NO_SITE_HELPSHIFT_FAQ_ID, NO_SITE_HELPSHIFT_FAQ_SECTION);
                break;
            case INVALID_URL:
                showError(R.string.invalid_site_url_message, null, null);
                break;
            case MISSING_XMLRPC_METHOD:
                showError(R.string.xmlrpc_missing_method_error,
                        MISSING_XMLRPC_METHOD_HELPSHIFT_FAQ_ID, MISSING_XMLRPC_METHOD_HELPSHIFT_FAQ_SECTION);
                break;
            case XMLRPC_BLOCKED:
                showError(R.string.xmlrpc_post_blocked_error,
                        XMLRPC_BLOCKED_HELPSHIFT_FAQ_ID, XMLRPC_BLOCKED_HELPSHIFT_FAQ_SECTION);
                break;
            case XMLRPC_FORBIDDEN:
                showError(R.string.xmlrpc_endpoint_forbidden_error,
                        XMLRPC_BLOCKED_HELPSHIFT_FAQ_ID, XMLRPC_BLOCKED_HELPSHIFT_FAQ_SECTION);
                break;
            case GENERIC_ERROR:
                showError(R.string.error_generic, null, null);
                break;
        }
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
    public void OnWPComSiteFetched(SiteStore.OnWPComSiteFetched event) {
        if (mRequestedSiteAddress == null) {
            // bail if user canceled
            return;
        }

        if (!isAdded()) {
            return;
        }

        if (event.isError()) {
            // not a wp.com site so, start the discovery process
            mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mRequestedSiteAddress));
        } else {
            if (event.site.isJetpackInstalled()) {
                // if Jetpack site, treat it as selfhosted and start the discovery process
                mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mRequestedSiteAddress));
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
    public void onDiscoverySucceeded(AccountStore.OnDiscoveryResponse event) {
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
            if (event.error == DiscoveryError.WORDPRESS_COM_SITE) {
                AppLog.e(T.API, "Inputted a wpcom address in site address screen.");

                // If the user is already logged in a wordpress.com account, bail out
                if (mAccountStore.hasAccessToken()) {
                    String currentUsername = mAccountStore.getAccount().getUserName();
                    AppLog.e(T.NUX, "User is already logged in WordPress.com: " + currentUsername);
                    mLoginListener.alreadyLoggedInWpcom();
                } else {
                    mLoginListener.gotWpcomSiteInfo(event.failedEndpoint, null, null);
                }

                return;
            } else {
                AppLog.e(T.API, "onDiscoveryResponse has error: " + event.error.name() + " - " + event.error.toString());
                handleDiscoveryError(event.error, event.failedEndpoint);
                return;
            }
        }

        AppLog.i(T.NUX, "Discovery succeeded, endpoint: " + event.xmlRpcEndpoint);
        mLoginListener.gotXmlRpcEndpoint(requestedSiteAddress, event.xmlRpcEndpoint);
    }
}
