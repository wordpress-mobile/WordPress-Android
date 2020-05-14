package org.wordpress.android.fluxc.store

import android.text.TextUtils
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.CENSORED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_SSL_CERTIFICATE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_AUTHENTICATED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NO_CONNECTION
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.TIMEOUT
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteHomepageRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteHomepageRestClient.UpdateHomepageResponse
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsError
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsErrorType
import org.wordpress.android.fluxc.store.Store.OnChangedError
import javax.inject.Inject

class SiteOptionsStore
@Inject constructor(siteHomepageRestClient: SiteHomepageRestClient){

    data class UpdateHomepagePayload(
        val response: UpdateHomepageResponse? = null
    ) : Payload<SiteOptionsError>() {
        constructor(error: WPComGsonNetworkError) : this() {
            this.error = error.toSiteOptionsError()
        }
    }

    class SiteOptionsError(var type: SiteOptionsErrorType, var message: String? = null) : OnChangedError

    enum class SiteOptionsErrorType {
        TIMEOUT,
        API_ERROR,
        INVALID_RESPONSE,
        AUTHORIZATION_REQUIRED,
        GENERIC_ERROR;
    }
}

fun WPComGsonNetworkError.toSiteOptionsError(): SiteOptionsError {
    val type = when (type) {
        TIMEOUT -> SiteOptionsErrorType.TIMEOUT
        NO_CONNECTION,
        SERVER_ERROR,
        INVALID_SSL_CERTIFICATE,
        NETWORK_ERROR -> SiteOptionsErrorType.API_ERROR
        PARSE_ERROR,
        NOT_FOUND,
        CENSORED,
        INVALID_RESPONSE -> SiteOptionsErrorType.INVALID_RESPONSE
        HTTP_AUTH_ERROR,
        AUTHORIZATION_REQUIRED,
        NOT_AUTHENTICATED -> SiteOptionsErrorType.AUTHORIZATION_REQUIRED
        UNKNOWN,
        null -> SiteOptionsErrorType.GENERIC_ERROR
    }
    return SiteOptionsError(type, message)
}
