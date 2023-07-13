package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.AcceptHeaderStrategy
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
import javax.inject.Singleton

@Singleton
class BlazeCampaignsRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    acceptStrategy: AcceptHeaderStrategy.JsonAcceptHeader
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent, acceptStrategy) {
    suspend fun fetchBlazeCampaigns(
        site: SiteModel,
        page: Int = 1
    ): BlazeCampaignsFetchedPayload<BlazeCampaignsResponse> {
        val url = "${WPCOMV2.sites.site(site.siteId).wordads.dsp.api.v1.search.campaigns.site.url}${site.siteId}"
        val response = wpComGsonRequestBuilder.syncGetRequest(
            this,
            url,
            mapOf(
                "page" to page.toString(),
                "order" to "start_date",
                "sort" to "desc"
            ),
            BlazeCampaignsResponse::class.java
        )
        return when (response) {
            is Success -> BlazeCampaignsFetchedPayload(response.data)
            is Error -> BlazeCampaignsFetchedPayload(response.error.toBlazeCampaignsError())
        }
    }
}

data class BlazeCampaignsFetchedPayload<T>(
    val response: T? = null
) : Payload<BlazeCampaignsError>() {
    constructor(error: BlazeCampaignsError) : this() {
        this.error = error
    }
}

class BlazeCampaignsError
@JvmOverloads constructor(
    val type: BlazeCampaignsErrorType,
    val message: String? = null
) : OnChangedError

enum class BlazeCampaignsErrorType {
    GENERIC_ERROR,
    AUTHORIZATION_REQUIRED,
    INVALID_RESPONSE,
    API_ERROR,
    TIMEOUT
}

fun WPComGsonNetworkError.toBlazeCampaignsError(): BlazeCampaignsError {
    val type = when (type) {
        GenericErrorType.TIMEOUT -> BlazeCampaignsErrorType.TIMEOUT
        GenericErrorType.NO_CONNECTION,
        GenericErrorType.SERVER_ERROR,
        GenericErrorType.INVALID_SSL_CERTIFICATE,
        GenericErrorType.NETWORK_ERROR -> BlazeCampaignsErrorType.API_ERROR

        GenericErrorType.PARSE_ERROR,
        GenericErrorType.NOT_FOUND,
        GenericErrorType.CENSORED,
        GenericErrorType.INVALID_RESPONSE -> BlazeCampaignsErrorType.INVALID_RESPONSE

        GenericErrorType.HTTP_AUTH_ERROR,
        GenericErrorType.AUTHORIZATION_REQUIRED,
        GenericErrorType.NOT_AUTHENTICATED -> BlazeCampaignsErrorType.AUTHORIZATION_REQUIRED

        GenericErrorType.UNKNOWN,
        null -> BlazeCampaignsErrorType.GENERIC_ERROR
    }
    return BlazeCampaignsError(type, message)
}
