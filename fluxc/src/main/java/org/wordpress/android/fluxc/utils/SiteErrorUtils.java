package org.wordpress.android.fluxc.utils;

import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError;
import org.wordpress.android.fluxc.store.SiteStore.SelfHostedErrorType;
import org.wordpress.android.fluxc.store.SiteStore.SiteError;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;
import org.wordpress.android.fluxc.store.SiteStore.WPAPIError;
import org.wordpress.android.fluxc.store.SiteStore.WPAPIErrorType;

public class SiteErrorUtils {
    public static SiteError genericToSiteError(BaseNetworkError error) {
        SiteErrorType errorType = SiteErrorType.GENERIC_ERROR;
        WPAPIError wpapiError = null;
        if (error.isGeneric()) {
            switch (error.type) {
                case INVALID_RESPONSE:
                    errorType = SiteErrorType.INVALID_RESPONSE;
                    break;
                case NOT_AUTHENTICATED:
                    errorType = SiteErrorType.NOT_AUTHENTICATED;
                    break;
            }
        }

        if (error instanceof WPAPINetworkError) {
            String errorCode = ((WPAPINetworkError) error).getErrorCode();
            wpapiError = new WPAPIError(
                    WPAPIErrorType.Companion.fromString(errorCode),
                    error.hasVolleyError() && error.volleyError.networkResponse != null
                            ? error.volleyError.networkResponse.statusCode
                            : 200 // volleyError will be missing when the request succeeds but the response is invalid
            );
        }

        SiteError siteError = new SiteError(errorType, error.message, SelfHostedErrorType.NOT_SET, wpapiError);

        switch (error.xmlRpcErrorType) {
            case METHOD_NOT_ALLOWED:
                siteError = new SiteError(errorType, error.message, SelfHostedErrorType.XML_RPC_SERVICES_DISABLED);
                break;
            case UNABLE_TO_READ_SITE:
                siteError = new SiteError(errorType, error.message, SelfHostedErrorType.UNABLE_TO_READ_SITE);
                break;
            case AUTH_REQUIRED:
            case NOT_SET:
            default:
                // Nothing to do
                break;
        }

        return siteError;
    }
}
