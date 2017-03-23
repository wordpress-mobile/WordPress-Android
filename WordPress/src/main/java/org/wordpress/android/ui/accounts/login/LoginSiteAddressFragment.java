package org.wordpress.android.ui.accounts.login;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.accounts.AbstractFragment;
import org.wordpress.android.ui.accounts.SignInDialogFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SelfSignedSSLUtils;
import org.wordpress.android.util.SelfSignedSSLUtils.Callback;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPUrlUtils;

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

import javax.inject.Inject;

public class LoginSiteAddressFragment extends AbstractFragment implements TextWatcher {
    public static final String TAG = "login_site_address_fragment_tag";

    private static final String XMLRPC_BLOCKED_HELPSHIFT_FAQ_SECTION = "10";
    private static final String XMLRPC_BLOCKED_HELPSHIFT_FAQ_ID = "102";

    private static final String MISSING_XMLRPC_METHOD_HELPSHIFT_FAQ_SECTION = "10";
    private static final String MISSING_XMLRPC_METHOD_HELPSHIFT_FAQ_ID = "11";

    private static final String NO_SITE_HELPSHIFT_FAQ_SECTION = "10";
    private static final String NO_SITE_HELPSHIFT_FAQ_ID = "2"; //using the same as in INVALID URL

    protected EditText mSiteAddressEditText;

    protected String mSiteAddress;

    protected Button mNextButton;
    protected View mDontKnowAddress;
    protected View mUrlExplanation;
    protected View mMoreHelp;

    protected @Inject SiteStore mSiteStore;
    protected @Inject AccountStore mAccountStore;
    protected @Inject Dispatcher mDispatcher;
    protected @Inject HTTPAuthManager mHTTPAuthManager;
    protected @Inject MemorizingTrustManager mMemorizingTrustManager;

    private OnSiteAddressRequestInteraction mListener;

    public interface OnSiteAddressRequestInteraction {
        void onSiteAddressRequestSuccess(String siteAddress, boolean isSelfHosted);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.login_address_screen, container, false);

        mSiteAddressEditText = (EditText) rootView.findViewById(R.id.site_address);
        mNextButton = (Button) rootView.findViewById(R.id.login_site_address_next_button);
        mNextButton.setOnClickListener(mNextClickListener);

        mDontKnowAddress = rootView.findViewById(R.id.forgot_address);
        mDontKnowAddress.setOnClickListener(mDontKnowSiteAddressListener);

        mUrlExplanation = rootView.findViewById(R.id.url_explanation);
        mMoreHelp = rootView.findViewById(R.id.more_help);

        mSiteAddressEditText.setOnEditorActionListener(mEditorAction);

        autofillFromBuildConfig();

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSiteAddressRequestInteraction) {
            mListener = (OnSiteAddressRequestInteraction) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnSiteAddressRequestInteraction");
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

        String siteAddress = (String) WordPress.getBuildConfigValue(getActivity().getApplication(),
                "DEBUG_LOGIN_SITE_ADDRESS");
        if (!TextUtils.isEmpty(siteAddress)) {
            mSiteAddressEditText.setText(siteAddress);
            AppLog.d(T.NUX, "Autofilled site address from build config");
        }
    }

    private boolean isWPComLogin() {
        String selfHostedUrl = EditTextUtils.getText(mSiteAddressEditText).trim();
        return TextUtils.isEmpty(selfHostedUrl) ||
                WPUrlUtils.isWordPressCom(UrlUtils.addUrlSchemeIfNeeded(selfHostedUrl, false));
    }

    private final View.OnClickListener mDontKnowSiteAddressListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mDontKnowAddress.setVisibility(View.GONE);
            mUrlExplanation.setVisibility(View.VISIBLE);
        }
    };

    protected void onDoneAction() {
        next();
    }

    private final TextView.OnEditorActionListener mEditorAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            return onDoneEvent(actionId, event);
        }
    };

    private void discoverBlog() {
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

    protected void next() {
        if (!isUserDataValid()) {
            return;
        }

        if (!checkNetworkConnectivity()) {
            return;
        }

        mSiteAddress = EditTextUtils.getText(mSiteAddressEditText).trim().toLowerCase();
        discoverBlog();
    }

    private final OnClickListener mNextClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            next();
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
            mNextButton.setEnabled(true);
        } else {
            mNextButton.setEnabled(false);
        }
        mSiteAddressEditText.setError(null);
    }

    private boolean fieldsFilled() {
        return EditTextUtils.getText(mSiteAddressEditText).trim().length() > 0;
    }

    protected boolean isUserDataValid() {
        final String url = EditTextUtils.getText(mSiteAddressEditText).trim();
        boolean retValue = true;

        if (TextUtils.isEmpty(url)) {
            mSiteAddressEditText.setError(getString(R.string.required_field));
            mSiteAddressEditText.requestFocus();
            retValue = false;
        }

        return retValue;
    }

    private void showUrlError(int messageId) {
        mSiteAddressEditText.setError(getString(messageId));
        mSiteAddressEditText.requestFocus();
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
        nuxAlert = SignInDialogFragment.newInstance(getString(org.wordpress.android.R.string.nux_cannot_log_in),
                errorMessage, R.drawable.ic_notice_white_64dp, 3,
                getString(R.string.cancel), getString(R.string.reader_title_applog), thirdButtonLabel,
                SignInDialogFragment.ACTION_OPEN_SUPPORT_CHAT,
                SignInDialogFragment.ACTION_OPEN_APPLICATION_LOG,
                faqAction, faqId, faqSection);
        Bundle bundle = nuxAlert.getArguments();
        bundle.putSerializable(HelpshiftHelper.ORIGIN_KEY,
                HelpshiftHelper.chooseHelpshiftLoginTag(false, isWPComLogin()));
        nuxAlert.setArguments(bundle);
        ft.add(nuxAlert, "alert");
        ft.commitAllowingStateLoss();
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

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDiscoverySucceeded(AccountStore.OnDiscoveryResponse event) {
        if (event.isError()) {
            AppLog.e(T.API, "onDiscoveryResponse has error: " + event.error.name() + " - " + event.error.toString());
            handleDiscoveryError(event.error, event.failedEndpoint);
            AnalyticsTracker.track(Stat.LOGIN_FAILED, event.getClass().getSimpleName(), event.error.name(), event.error.toString());
            return;
        }
        AppLog.i(T.NUX, "Discovery succeeded, endpoint: " + event.xmlRpcEndpoint);
//        SiteStore.RefreshSitesXMLRPCPayload selfhostedPayload = new SiteStore.RefreshSitesXMLRPCPayload();
//        selfhostedPayload.username = mUsername;
//        selfhostedPayload.password = mPassword;
//        selfhostedPayload.url = event.xmlRpcEndpoint;
//        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(selfhostedPayload));

        mListener.onSiteAddressRequestSuccess(mSiteAddress, true);
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
                mListener.onSiteAddressRequestSuccess(mSiteAddress, false);
                break;
            case NO_SITE_ERROR:
                showGenericErrorDialog(getResources().getString(R.string.no_site_error),
                        NO_SITE_HELPSHIFT_FAQ_ID,
                        NO_SITE_HELPSHIFT_FAQ_SECTION);
                break;
            case INVALID_URL:
                showUrlError(R.string.invalid_site_url_message);
                AnalyticsTracker.track(Stat.LOGIN_INSERTED_INVALID_URL);
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
}
