package org.wordpress.android.login;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.fluxc.store.AccountStore.OnDiscoveryResponse;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;

public abstract class LoginBaseDiscoveryFragment extends LoginBaseFormFragment<LoginListener> {
    LoginBaseDiscoveryListener mLoginBaseDiscoveryListener;

    public interface LoginBaseDiscoveryListener {
        String getRequestedSiteAddress();
        void handleWpComDiscoveryError(String failedEndpoint);
        void handleDiscoverySuccess(String endpointAddress);
        void handleDiscoveryError(DiscoveryError error, String failedEndpoint);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginBaseDiscoveryListener = null;
    }

    void initiateDiscovery() {
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        // Start the discovery process
        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(
                mLoginBaseDiscoveryListener.getRequestedSiteAddress()));
    }

    /**
     *
     * @param error - the error type returned from the discovery API
     * @return boolean flag - if the error can occur even with Jetpack available on the site
     */
    boolean canErrorOccurWithJetpackEnabled(DiscoveryError error) {
        return (error == DiscoveryError.GENERIC_ERROR
            || error == DiscoveryError.XMLRPC_FORBIDDEN
            || error == DiscoveryError.XMLRPC_BLOCKED
            || error == DiscoveryError.MISSING_XMLRPC_METHOD
            || error == DiscoveryError.NO_SITE_ERROR);
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
        if (error == DiscoveryError.WORDPRESS_COM_SITE) {
            mLoginBaseDiscoveryListener.handleWpComDiscoveryError(failedEndpoint);
        } else {
            mLoginBaseDiscoveryListener.handleDiscoveryError(error, failedEndpoint);
        }
    }
}
