package org.wordpress.android.fluxc.network.rest.wpcom.mobile

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.RemoteConfigErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.Store.OnChangedError
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RemoteConfigClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchRemoteConfig(): RemoteConfigFetchedPayload {
        // https://public-api.wordpress.com/wpcom/v2/mobile/remote_config
        val url = WPCOMV2.mobile.remote_config.url
        val response = wpComGsonRequestBuilder.syncGetRequest(
            this,
            url,
            mapOf(),
            Map::class.java
        )
        return when (response) {
            is Response.Success -> buildRemoteConfigFetchedPayload(response.data)
            is Response.Error -> RemoteConfigFetchedPayload(response.error.toRemoteConfigError())
        }
    }

    private fun buildRemoteConfigFetchedPayload(featureFlags: Map<*, *>?)
        : RemoteConfigFetchedPayload {
        return RemoteConfigFetchedPayload(featureFlags?.map { e ->
                e.key.toString() to e.value.toString().toBoolean()
            }?.toMap())
    }
}

data class RemoteConfigFetchedPayload (
    val remoteConfig: Map<String, Boolean>? = null
) : Payload<RemoteConfigError>() {
    constructor(error: RemoteConfigError) : this() {
        this.error = error
    }
}

class RemoteConfigError(
    val type: RemoteConfigErrorType,
    val message: String? = null
) : OnChangedError

enum class RemoteConfigErrorType {
    API_ERROR,
    GENERIC_ERROR,
    INVALID_RESPONSE,
    TIMEOUT,
}

fun WPComGsonNetworkError.toRemoteConfigError(): RemoteConfigError {
    val type = when (type) {
        GenericErrorType.TIMEOUT -> RemoteConfigErrorType.TIMEOUT
        GenericErrorType.NO_CONNECTION,
        GenericErrorType.SERVER_ERROR,
        GenericErrorType.INVALID_SSL_CERTIFICATE,
        GenericErrorType.NETWORK_ERROR -> RemoteConfigErrorType.API_ERROR
        GenericErrorType.PARSE_ERROR,
        GenericErrorType.NOT_FOUND,
        GenericErrorType.CENSORED,
        GenericErrorType.INVALID_RESPONSE -> RemoteConfigErrorType.INVALID_RESPONSE
        GenericErrorType.UNKNOWN -> GENERIC_ERROR
        else -> GENERIC_ERROR
    }
    return RemoteConfigError(type, message)
}
