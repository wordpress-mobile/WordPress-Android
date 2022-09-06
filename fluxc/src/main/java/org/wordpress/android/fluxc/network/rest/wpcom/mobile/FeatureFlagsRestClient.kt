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
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.Store.OnChangedError
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class FeatureFlagsRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchFeatureFlags(
        deviceId: String,
        identifier: String,
        platform: String = "android"
        ): FeatureFlagsFetchedPayload {
        // https://public-api.wordpress.com/wpcom/v2/mobile/feature-flagsdevice_id=12345&platform=android&build_number=570&marketing_version=15.1.1&identifier=com.jetpack.android
        val url = WPCOMV2.mobile.feature_flags.url
        val params = mapOf(
            "device_id" to deviceId,
            "identifier" to identifier,
            "platform" to platform
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
            this,
            url,
            params,
            Map::class.java
        )
        return when (response) {
            is Response.Success -> buildFeatureFlagsFetchedPayload(response.data)
            is Response.Error -> FeatureFlagsFetchedPayload(response.error.toFeatureFlagsError())
        }
    }

    private fun buildFeatureFlagsFetchedPayload(featureFlags: Map<*, *>?)
        : FeatureFlagsFetchedPayload {
        return FeatureFlagsFetchedPayload(featureFlags?.map { e ->
                e.key.toString() to e.value.toString().toBoolean()
            }?.toMap())
    }
}

data class FeatureFlagsFetchedPayload (
    val featureFlags: Map<String, Boolean>? = null
) : Payload<FeatureFlagsError>() {
    constructor(error: FeatureFlagsError) : this() {
        this.error = error
    }
}

class FeatureFlagsError(
    val type: FeatureFlagsErrorType,
    val message: String? = null
) : OnChangedError

enum class FeatureFlagsErrorType {
    API_ERROR,
    AUTH_ERROR,
    GENERIC_ERROR,
    INVALID_RESPONSE,
    TIMEOUT,
}

fun WPComGsonNetworkError.toFeatureFlagsError(): FeatureFlagsError {
    val type = when (type) {
        GenericErrorType.TIMEOUT -> FeatureFlagsErrorType.TIMEOUT
        GenericErrorType.NO_CONNECTION,
        GenericErrorType.SERVER_ERROR,
        GenericErrorType.INVALID_SSL_CERTIFICATE,
        GenericErrorType.NETWORK_ERROR -> FeatureFlagsErrorType.API_ERROR
        GenericErrorType.PARSE_ERROR,
        GenericErrorType.NOT_FOUND,
        GenericErrorType.CENSORED,
        GenericErrorType.INVALID_RESPONSE -> FeatureFlagsErrorType.INVALID_RESPONSE
        GenericErrorType.HTTP_AUTH_ERROR,
        GenericErrorType.AUTHORIZATION_REQUIRED,
        GenericErrorType.NOT_AUTHENTICATED -> FeatureFlagsErrorType.AUTH_ERROR
        GenericErrorType.UNKNOWN -> GENERIC_ERROR
        null -> GENERIC_ERROR
    }
    return FeatureFlagsError(type, message)
}
