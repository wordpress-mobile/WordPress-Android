package org.wordpress.android.fluxc.network.rest.wpcom.qrcode

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
class QrcodeRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun validate(data: String, token: String): QrcodePayload<QrcodeValidateResponse> {
        val url = WPCOMV2.auth.qr_code.validate.url
        val params = mapOf("data" to data, "token" to token)
        return when (val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                params,
                null,
                QrcodeValidateResponse::class.java
        )) {
            is Response.Error -> {
                QrcodePayload(response.error.toQrcodeError())
            }
            is Response.Success -> QrcodePayload(response.data)
        }
    }

    suspend fun authenticate(data: String, token: String): QrcodePayload<QrcodeAuthenticateResponse> {
        val url = WPCOMV2.auth.qr_code.authenticate.url
        val params = mapOf("data" to data, "token" to token)
        return when (val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                params,
                null,
                QrcodeAuthenticateResponse::class.java
        )) {
            is Response.Error -> {
                QrcodePayload(response.error.toQrcodeError())
            }
            is Response.Success -> QrcodePayload(response.data)
        }
    }

    data class QrcodeValidateResponse(
        @SerializedName("browser") val browser: String? = null,
        @SerializedName("location") val location: String? = null,
        @SerializedName("success") val success: Boolean? = null,
        @SerializedName("error") val error: String? = null
    )

    data class QrcodeAuthenticateResponse(
        @SerializedName("authenticated") val authenticated: Boolean? = null,
        @SerializedName("error") val error: String? = null
    )
}

data class QrcodePayload<T>(
    val response: T? = null
) : Payload<QrcodeError>() {
    constructor(error: QrcodeError) : this() {
        this.error = error
    }
}

class QrcodeError(
    val type: QrcodeErrorType,
    val message: String? = null
) : OnChangedError

enum class QrcodeErrorType {
    GENERIC_ERROR,
    AUTHORIZATION_REQUIRED,
    INVALID_RESPONSE,
    REST_INVALID_PARAM,
    DATA_INVALID,
    API_ERROR,
    TIMEOUT,
    NOT_AUTHORIZED
}

fun WPComGsonNetworkError.toQrcodeError(): QrcodeError {
    val type = when (type) {
        GenericErrorType.TIMEOUT -> QrcodeErrorType.TIMEOUT
        GenericErrorType.NO_CONNECTION,
        GenericErrorType.SERVER_ERROR,
        GenericErrorType.INVALID_SSL_CERTIFICATE,
        GenericErrorType.NETWORK_ERROR -> QrcodeErrorType.API_ERROR
        GenericErrorType.PARSE_ERROR,
        GenericErrorType.NOT_FOUND,
        GenericErrorType.CENSORED,
        GenericErrorType.INVALID_RESPONSE -> QrcodeErrorType.INVALID_RESPONSE
        GenericErrorType.HTTP_AUTH_ERROR,
        GenericErrorType.AUTHORIZATION_REQUIRED,
        GenericErrorType.NOT_AUTHENTICATED -> QrcodeErrorType.AUTHORIZATION_REQUIRED
        GenericErrorType.UNKNOWN -> {
            when (apiError) {
                QrcodeErrorType.REST_INVALID_PARAM.name.lowercase() -> {
                    QrcodeErrorType.REST_INVALID_PARAM
                }
                QrcodeErrorType.DATA_INVALID.name.lowercase() -> {
                    QrcodeErrorType.DATA_INVALID
                }
                QrcodeErrorType.NOT_AUTHORIZED.name.lowercase() -> {
                    QrcodeErrorType.NOT_AUTHORIZED
                }
                else -> {
                    QrcodeErrorType.GENERIC_ERROR
                }
            }
        }
        null -> QrcodeErrorType.GENERIC_ERROR
    }
    return QrcodeError(type, message)
}
