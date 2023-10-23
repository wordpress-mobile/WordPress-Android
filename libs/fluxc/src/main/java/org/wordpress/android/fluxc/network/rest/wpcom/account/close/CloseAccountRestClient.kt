package org.wordpress.android.fluxc.network.rest.wpcom.account.close

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CloseAccountRestClient @Inject constructor(
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun closeAccount(): CloseAccountWPAPIPayload<Unit> {
        val url = WPCOMREST.me.account.close.urlV1_1
        val response = wpComGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            params = emptyMap(),
            body = emptyMap(),
            clazz = Unit::class.java
        )
        return when (response) {
            is Success -> CloseAccountWPAPIPayload(Unit)
            is Error -> CloseAccountWPAPIPayload(response.error)
        }
    }

    data class CloseAccountWPAPIPayload<T>(
        val result: T?
    ) : Payload<WPComGsonNetworkError?>() {
        constructor(error: WPComGsonNetworkError) : this(null) {
            this.error = error
        }
    }
}
