package org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
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
import org.wordpress.android.fluxc.store.Store.OnChangedError
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class QRCodeAuthRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun validate(data: String, token: String): QRCodeAuthPayload<QRCodeAuthValidateResponse> {
        val url = WPCOMV2.auth.qr_code.validate.url
        val params = mapOf("data" to data, "token" to token)
        val response = wpComGsonRequestBuilder.syncPostRequest(
            this,
            url,
            params,
            null,
            QRCodeAuthValidateResponse::class.java
        )
        return when (response) {
            is Response.Success -> QRCodeAuthPayload(response.data)
            is Response.Error -> QRCodeAuthPayload(response.error.toQrcodeError())
        }
    }

    suspend fun authenticate(data: String, token: String): QRCodeAuthPayload<QRCodeAuthAuthenticateResponse> {
        val url = WPCOMV2.auth.qr_code.authenticate.url
        val params = mapOf("data" to data, "token" to token)
        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                params,
                null,
                QRCodeAuthAuthenticateResponse::class.java
        )
        return when (response) {
            is Response.Success -> QRCodeAuthPayload(response.data)
            is Response.Error -> QRCodeAuthPayload(response.error.toQrcodeError())
        }
    }

    data class QRCodeAuthValidateResponse(
        @SerializedName("browser") val browser: String? = null,
        @SerializedName("location") val location: String? = null,
        @SerializedName("success") val success: Boolean? = null
    )

    data class QRCodeAuthAuthenticateResponse(
        @SerializedName("authenticated") val authenticated: Boolean? = null
    )
}

data class QRCodeAuthPayload<T>(
    val response: T? = null
) : Payload<QRCodeAuthError>() {
    constructor(error: QRCodeAuthError) : this() {
        this.error = error
    }
}

class QRCodeAuthError(
    val type: QRCodeAuthErrorType,
    val message: String? = null
) : OnChangedError

enum class QRCodeAuthErrorType {
    GENERIC_ERROR,
    AUTHORIZATION_REQUIRED,
    INVALID_RESPONSE,
    REST_INVALID_PARAM,
    DATA_INVALID,
    API_ERROR,
    TIMEOUT,
    NOT_AUTHORIZED
}

fun WPComGsonNetworkError.toQrcodeError(): QRCodeAuthError {
    val type = when (type) {
        GenericErrorType.TIMEOUT -> QRCodeAuthErrorType.TIMEOUT
        GenericErrorType.NO_CONNECTION,
        GenericErrorType.SERVER_ERROR,
        GenericErrorType.INVALID_SSL_CERTIFICATE,
        GenericErrorType.NETWORK_ERROR -> QRCodeAuthErrorType.API_ERROR
        GenericErrorType.PARSE_ERROR,
        GenericErrorType.NOT_FOUND,
        GenericErrorType.CENSORED,
        GenericErrorType.INVALID_RESPONSE -> QRCodeAuthErrorType.INVALID_RESPONSE
        GenericErrorType.HTTP_AUTH_ERROR,
        GenericErrorType.AUTHORIZATION_REQUIRED,
        GenericErrorType.NOT_AUTHENTICATED -> QRCodeAuthErrorType.AUTHORIZATION_REQUIRED
        GenericErrorType.UNKNOWN -> {
            when (apiError) {
                QRCodeAuthErrorType.REST_INVALID_PARAM.name.lowercase() -> {
                    QRCodeAuthErrorType.REST_INVALID_PARAM
                }
                QRCodeAuthErrorType.DATA_INVALID.name.lowercase() -> {
                    QRCodeAuthErrorType.DATA_INVALID
                }
                QRCodeAuthErrorType.NOT_AUTHORIZED.name.lowercase() -> {
                    QRCodeAuthErrorType.NOT_AUTHORIZED
                }
                else -> {
                    QRCodeAuthErrorType.GENERIC_ERROR
                }
            }
        }
        null -> QRCodeAuthErrorType.GENERIC_ERROR
    }
    return QRCodeAuthError(type, message)
}
