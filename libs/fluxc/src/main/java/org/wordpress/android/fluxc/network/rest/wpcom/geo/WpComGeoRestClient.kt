package org.wordpress.android.fluxc.network.rest.wpcom.geo

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WpComGeoRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchCountryCode(): Result<String?> {
        val url = WPCOMREST.geo.urlV0
        val response = wpComGsonRequestBuilder.syncGetRequest(
            this,
            url,
            emptyMap(),
            GeoResponse::class.java
        )
        return when (response) {
            is WPComGsonRequestBuilder.Response.Success -> Result.success(response.data.countryCode)
            is WPComGsonRequestBuilder.Response.Error -> Result.failure(response.error.volleyError)
        }
    }
}

data class GeoResponse(
    @SerializedName("country_short")
    val countryCode: String? = null
)
