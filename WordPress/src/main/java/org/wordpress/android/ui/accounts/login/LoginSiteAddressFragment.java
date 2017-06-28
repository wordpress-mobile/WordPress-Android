package org.wordpress.android.ui.accounts.login;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Response;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteWPComRestResponse;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SelfSignedSSLUtils;
import org.wordpress.android.util.SelfSignedSSLUtils.Callback;
import org.wordpress.android.util.UrlUtils;

import javax.inject.Inject;

public class LoginSiteAddressFragment extends Fragment implements TextWatcher {
    private static final String KEY_IN_PROGRESS = "KEY_IN_PROGRESS";
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
    private Button mNextButton;
    private ProgressDialog mProgressDialog;

    private LoginListener mLoginListener;

    private boolean mInProgress;
    private String mRequestedSiteAddress;

    @Inject SiteRestClient mSiteRestClient;
    @Inject AccountStore mAccountStore;
    @Inject Dispatcher mDispatcher;
    @Inject HTTPAuthManager mHTTPAuthManager;
    @Inject MemorizingTrustManager mMemorizingTrustManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.login_site_address_screen, container, false);

        mSiteAddressEditText = (EditText) rootView.findViewById(R.id.login_site_address);
        mSiteAddressEditText.addTextChangedListener(this);

        mSiteAddressTextLayout = (TextInputLayout) rootView.findViewById(R.id.login_site_address_layout);

        mNextButton = (Button) rootView.findViewById(R.id.login_next_button);
        mNextButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                discover();
            }
        });

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

        rootView.findViewById(R.id.login_site_address_help).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showSiteAddressHelp();
            }
        });

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mInProgress = savedInstanceState.getBoolean(KEY_IN_PROGRESS);
            mRequestedSiteAddress = savedInstanceState.getString(KEY_REQUESTED_SITE_ADDRESS);

            if (mInProgress) {
                showProgressDialog();
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LoginListener) {
            mLoginListener = (LoginListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement LoginListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_IN_PROGRESS, mInProgress);
        outState.putString(KEY_REQUESTED_SITE_ADDRESS, mRequestedSiteAddress);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_login, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.help) {
            mLoginListener.help();
            return true;
        }

        return false;
    }

    protected void discover() {
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        mRequestedSiteAddress = getCleanedSiteAddress();

        Uri uri = Uri.parse(UrlUtils.addUrlSchemeIfNeeded(mRequestedSiteAddress, false));
        String endpoint = WPCOMREST.sites.getUrlV1() + uri.getHost();
        WPComGsonRequest req = WPComGsonRequest.buildGetRequest(endpoint, null, SiteWPComRestResponse.class,
                new Response.Listener<SiteWPComRestResponse>() {
                    @Override
                    public void onResponse(SiteWPComRestResponse response) {
                        if (response.jetpack) {
                            // if Jetpack site, treat it as selfhosted and start the discovery process
                            mDispatcher.dispatch(
                                    AuthenticationActionBuilder.newDiscoverEndpointAction(mRequestedSiteAddress));
                            return;
                        }

                        endProgress();

                        // it's a wp.com site so, treat it as such.
                        mLoginListener.gotWpcomSiteInfo(
                                UrlUtils.removeScheme(response.URL),
                                response.name,
                                response.icon == null ? null : response.icon.img);
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError baseNetworkError) {
                        if (baseNetworkError.type != null && baseNetworkError.type == GenericErrorType.NOT_FOUND) {
                            // not a wp.com site so, start the discovery process
                            mDispatcher.dispatch(
                                    AuthenticationActionBuilder.newDiscoverEndpointAction(mRequestedSiteAddress));
                        } else {
                            showError(R.string.no_site_error, NO_SITE_HELPSHIFT_FAQ_ID, NO_SITE_HELPSHIFT_FAQ_SECTION);
                            endProgress();
                        }
                    }
                });
        mSiteRestClient.add(req);

        // auth token can be invalid so, clean it up for this public API call
        req.setAccessToken(null);

        showProgressDialog();
    }

    private String getCleanedSiteAddress() {
        return EditTextUtils.getText(mSiteAddressEditText).trim().replaceAll("\r|\n", "");
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        updateNextButton();
        mSiteAddressTextLayout.setError(null);
        mLoginListener.setHelpContext(null, null);
    }

    private void updateNextButton() {
        mNextButton.setEnabled(getCleanedSiteAddress().length() > 0);
    }

    private void showError(int messageId, String faqId, String faqSection) {
        mSiteAddressTextLayout.setError(getString(messageId));
        mLoginListener.setHelpContext(faqId, faqSection);
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mDispatcher.unregister(this);
    }

    private void showProgressDialog() {
        mNextButton.setEnabled(false);
        mProgressDialog = ProgressDialog.show(
                getActivity(),
                "",
                getActivity().getString(R.string.login_checking_site_address),
                true,
                true,
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        if (mInProgress) {
                            endProgress();
                        }
                    }
                });
        mInProgress = true;
    }

    private void endProgress() {
        mInProgress = false;

        if (mProgressDialog != null) {
            mProgressDialog.cancel();
            mProgressDialog = null;
        }

        mRequestedSiteAddress = null;

        updateNextButton();
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

        if (mInProgress) {
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
