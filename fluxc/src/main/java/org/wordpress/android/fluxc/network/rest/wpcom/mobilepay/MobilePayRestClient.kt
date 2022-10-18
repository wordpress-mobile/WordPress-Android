package org.wordpress.android.fluxc.network.rest.wpcom.mobilepay

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.Store
import javax.inject.Inject
import javax.inject.Named

private const val APP_ID_HEADER = "X-APP-ID"

class MobilePayRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {

    suspend fun createOrder(
        productIdentifier: String,
        price: Long,
        currency: String,
        purchaseToken: String,
        appId: String,
    ): CreateOrderPayload {
        val response = wpComGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = WPCOMV2.iap.orders.url,
            params = null,
            body = mapOf(
                "product_id" to productIdentifier,
                "price" to price,
                "currency" to currency,
                "purchaseToken" to purchaseToken,
            ),
            clazz = CreateOrderResponse::class.java,
            headers = mapOf(APP_ID_HEADER to appId)
        )
        return when (response) {
            is Response.Success -> CreateOrderPayload(response.data.orderId)
            is Response.Error -> CreateOrderPayload(response.error.toCreateOrderError())
        }
    }

    data class CreateOrderResponse(
        val orderId: Long
    )

    data class CreateOrderPayload(
        val orderId: Long? = null
    ) : Payload<CreateOrderError>() {
        constructor(error: CreateOrderError) : this() {
            this.error = error
        }
    }

    class CreateOrderError(
        val type: CreateOrderErrorType,
        val message: String? = null
    ) : Store.OnChangedError

    enum class CreateOrderErrorType {
        API_ERROR,
        AUTH_ERROR,
        GENERIC_ERROR,
        INVALID_RESPONSE,
        TIMEOUT,
    }

    private fun WPComGsonRequest.WPComGsonNetworkError.toCreateOrderError(): CreateOrderError {
        val type = when (type) {
            BaseRequest.GenericErrorType.TIMEOUT -> CreateOrderErrorType.TIMEOUT
            BaseRequest.GenericErrorType.NO_CONNECTION,
            BaseRequest.GenericErrorType.SERVER_ERROR,
            BaseRequest.GenericErrorType.INVALID_SSL_CERTIFICATE,
            BaseRequest.GenericErrorType.NETWORK_ERROR -> CreateOrderErrorType.API_ERROR
            BaseRequest.GenericErrorType.PARSE_ERROR,
            BaseRequest.GenericErrorType.NOT_FOUND,
            BaseRequest.GenericErrorType.CENSORED,
            BaseRequest.GenericErrorType.INVALID_RESPONSE -> CreateOrderErrorType.INVALID_RESPONSE
            BaseRequest.GenericErrorType.HTTP_AUTH_ERROR,
            BaseRequest.GenericErrorType.AUTHORIZATION_REQUIRED,
            BaseRequest.GenericErrorType.NOT_AUTHENTICATED -> CreateOrderErrorType.AUTH_ERROR
            BaseRequest.GenericErrorType.UNKNOWN -> CreateOrderErrorType.GENERIC_ERROR
            null -> CreateOrderErrorType.GENERIC_ERROR
        }
        return CreateOrderError(type, message)
    }
}