package org.wordpress.android.fluxc.utils;

import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.store.SiteStore.SelfHostedErrorType;
import org.wordpress.android.fluxc.store.SiteStore.SiteError;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;

public class SiteErrorUtils {
    public static SiteError genericToSiteError(BaseNetworkError error) {
        SiteErrorType errorType = SiteErrorType.GENERIC_ERROR;
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

        SiteError siteError = new SiteError(errorType, error.message);

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
