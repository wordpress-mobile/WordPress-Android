package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.Store.OnChangedError
import javax.inject.Inject
import javax.inject.Named

class BlazeRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchBlazeStatus(site: SiteModel): BlazeStatusFetchedPayload {
        val url = WPCOMV2.sites.site(site.siteId).blaze.status.url
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, mapOf(), Map::class.java)
        return when (response) {
            is Success -> buildBlazeStatusFetchedPayload(site, response.data)
            is Error -> BlazeStatusFetchedPayload(site.siteId, response.error.toBlazeStatusError())
        }
    }

    private fun buildBlazeStatusFetchedPayload(
        site: SiteModel,
        data: Map<*, *>?
    ): BlazeStatusFetchedPayload {
        return BlazeStatusFetchedPayload(site.siteId, data?.map { e ->
            e.key.toString() to e.value.toString().toBoolean()
        }?.toMap())
    }
}

data class BlazeStatusFetchedPayload(
    val siteId: Long,
    val eligibility: Map<String, Boolean>? = null
) : Payload<BlazeStatusError>() {
    constructor(siteId: Long, error: BlazeStatusError) : this(siteId) {
        this.error = error
    }
}

class BlazeStatusError
@JvmOverloads constructor(
    val type: BlazeStatusErrorType,
    val message: String? = null
) : OnChangedError

enum class BlazeStatusErrorType {
    GENERIC_ERROR,
    AUTHORIZATION_REQUIRED,
    INVALID_RESPONSE,
    API_ERROR,
    TIMEOUT
}

fun WPComGsonNetworkError.toBlazeStatusError(): BlazeStatusError {
    val type = when (type) {
        GenericErrorType.TIMEOUT -> BlazeStatusErrorType.TIMEOUT
        GenericErrorType.NO_CONNECTION,
        GenericErrorType.SERVER_ERROR,
        GenericErrorType.INVALID_SSL_CERTIFICATE,
        GenericErrorType.NETWORK_ERROR -> BlazeStatusErrorType.API_ERROR
        GenericErrorType.PARSE_ERROR,
        GenericErrorType.NOT_FOUND,
        GenericErrorType.CENSORED,
        GenericErrorType.INVALID_RESPONSE -> BlazeStatusErrorType.INVALID_RESPONSE
        GenericErrorType.HTTP_AUTH_ERROR,
        GenericErrorType.AUTHORIZATION_REQUIRED,
        GenericErrorType.NOT_AUTHENTICATED -> BlazeStatusErrorType.AUTHORIZATION_REQUIRED
        GenericErrorType.UNKNOWN,
        null -> BlazeStatusErrorType.GENERIC_ERROR
    }
    return BlazeStatusError(type, message)
}
