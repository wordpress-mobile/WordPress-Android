package org.wordpress.android.login;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.fluxc.store.AccountStore.OnDiscoveryResponse;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;

import javax.inject.Inject;

public abstract class LoginBaseDiscoveryFragment extends LoginBaseFormFragment<LoginListener> {
    @Inject HTTPAuthManager mHTTPAuthManager;
    @Inject MemorizingTrustManager mMemorizingTrustManager;

    LoginBaseDiscoveryListener mLoginBaseDiscoveryListener;

    public interface LoginBaseDiscoveryListener {
        String getRequestedSiteAddress();
        void showDiscoveryError(int messageId);
        void handleWpComDiscoveryError(String failedEndpoint);
        void handleDiscoverySuccess(String endpointAddress);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginBaseDiscoveryListener = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LoginHttpAuthDialogFragment.DO_HTTP_AUTH && resultCode == Activity.RESULT_OK) {
            String url = data.getStringExtra(LoginHttpAuthDialogFragment.ARG_URL);
            String httpUsername = data.getStringExtra(LoginHttpAuthDialogFragment.ARG_USERNAME);
            String httpPassword = data.getStringExtra(LoginHttpAuthDialogFragment.ARG_PASSWORD);
            mHTTPAuthManager.addHTTPAuthCredentials(httpUsername, httpPassword, url, null);
            initiateDiscovery();
        }
    }

    void initiateDiscovery() {
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        // Start the discovery process
        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(
                mLoginBaseDiscoveryListener.getRequestedSiteAddress()));
    }

    private void askForHttpAuthCredentials(@NonNull final String url) {
        LoginHttpAuthDialogFragment loginHttpAuthDialogFragment = LoginHttpAuthDialogFragment.newInstance(url);
        loginHttpAuthDialogFragment.setTargetFragment(this, LoginHttpAuthDialogFragment.DO_HTTP_AUTH);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            loginHttpAuthDialogFragment.show(getFragmentManager(), LoginHttpAuthDialogFragment.TAG);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDiscoverySucceeded(OnDiscoveryResponse event) {
        // hold the URL in a variable to use below otherwise it gets cleared up by endProgress
        // bail if user canceled
        String mRequestedSiteAddress = mLoginBaseDiscoveryListener.getRequestedSiteAddress();
        if (mRequestedSiteAddress == null) {
            return;
        }

        if (!isAdded()) {
            return;
        }

        if (event.isError()) {
            if (isInProgress()) {
                endProgress();
            }

            mAnalyticsListener.trackLoginFailed(event.getClass().getSimpleName(),
                    event.error.name(), event.error.toString());

            AppLog.e(T.API, "onDiscoveryResponse has error: " + event.error.name()
                            + " - " + event.error.toString());
            handleDiscoveryError(event.error, event.failedEndpoint);
            return;
        }

        AppLog.i(T.NUX, "Discovery succeeded, endpoint: " + event.xmlRpcEndpoint);
        mLoginBaseDiscoveryListener.handleDiscoverySuccess(event.xmlRpcEndpoint);
    }

    private void handleDiscoveryError(DiscoveryError error, final String failedEndpoint) {
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
                                initiateDiscovery();
                            }
                        });
                break;
            case HTTP_AUTH_REQUIRED:
                askForHttpAuthCredentials(failedEndpoint);
                break;
            case NO_SITE_ERROR:
                mLoginBaseDiscoveryListener.showDiscoveryError(R.string.no_site_error);
                break;
            case INVALID_URL:
                mLoginBaseDiscoveryListener.showDiscoveryError(R.string.invalid_site_url_message);
                mAnalyticsListener.trackInsertedInvalidUrl();
                break;
            case MISSING_XMLRPC_METHOD:
                mLoginBaseDiscoveryListener.showDiscoveryError(R.string.xmlrpc_missing_method_error);
                break;
            case XMLRPC_BLOCKED:
                mLoginBaseDiscoveryListener.showDiscoveryError(R.string.xmlrpc_post_blocked_error);
                break;
            case XMLRPC_FORBIDDEN:
                mLoginBaseDiscoveryListener.showDiscoveryError(R.string.xmlrpc_endpoint_forbidden_error);
                break;
            case GENERIC_ERROR:
                mLoginBaseDiscoveryListener.showDiscoveryError(R.string.error_generic);
                break;
            case WORDPRESS_COM_SITE:
                mLoginBaseDiscoveryListener.handleWpComDiscoveryError(failedEndpoint);
                break;
        }
    }
}
