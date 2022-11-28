package org.wordpress.android.fluxc.network.rest.wpcom.mobilepay

import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.mobilepay.MobilePayRestClient.CreateOrderErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.mobilepay.MobilePayRestClient.CreateOrderResponse
import org.wordpress.android.fluxc.network.rest.wpcom.mobilepay.MobilePayRestClient.CreateOrderResponseType
import org.wordpress.android.fluxc.test

class MobilePayRestClientTest {
    private var wpComGsonRequestBuilder: WPComGsonRequestBuilder = mock()
    private var dispatcher: Dispatcher = mock()
    private var requestQueue: RequestQueue = mock()
    private var accessToken: AccessToken = mock()
    private var userAgent: UserAgent = mock()

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var bodyCaptor: KArgumentCaptor<Map<String, Any>>
    private lateinit var headersCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: MobilePayRestClient

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        bodyCaptor = argumentCaptor()
        headersCaptor = argumentCaptor()
        restClient = MobilePayRestClient(
            wpComGsonRequestBuilder,
            null,
            dispatcher,
            requestQueue,
            accessToken,
            userAgent
        )
    }

    @Test
    fun `given custom url, when create order, then correct custom url used`() = test {
        // GIVEN
        val customUrl = "https://custom.url"
        initResponse()

        // WHEN
        restClient.createOrder(
            productIdentifier = PRODUCT_IDENTIFIER,
            priceInCents = PRICE_IN_CENTS,
            currency = CURRENCY,
            purchaseToken = PURCHASE_TOKEN,
            appId = APP_ID,
            siteId = SITE_ID,
            customBaseUrl = customUrl
        )

        // THEN
        assertThat(urlCaptor.firstValue).isEqualTo(
            "$customUrl/wpcom/v2/iap/orders/"
        )
    }

    @Test
    fun `given standard url, when create order, then correct standard url used`() = test {
        // GIVEN
        initResponse()

        // WHEN
        restClient.createOrder(
            productIdentifier = PRODUCT_IDENTIFIER,
            priceInCents = PRICE_IN_CENTS,
            currency = CURRENCY,
            purchaseToken = PURCHASE_TOKEN,
            appId = APP_ID,
            siteId = SITE_ID,
            customBaseUrl = null
        )

        // THEN
        assertThat(urlCaptor.firstValue).isEqualTo(
            "https://public-api.wordpress.com/wpcom/v2/iap/orders/"
        )
    }

    @Test
    fun `given params, when create order, then correct body used`() = test {
        // GIVEN
        initResponse()

        // WHEN
        restClient.createOrder(
            productIdentifier = PRODUCT_IDENTIFIER,
            priceInCents = PRICE_IN_CENTS,
            currency = CURRENCY,
            purchaseToken = PURCHASE_TOKEN,
            appId = APP_ID,
            siteId = SITE_ID,
            customBaseUrl = null
        )

        // THEN
        assertThat(bodyCaptor.firstValue["currency"]).isEqualTo(CURRENCY)
        assertThat(bodyCaptor.firstValue["price"]).isEqualTo(PRICE_IN_CENTS)
        assertThat(bodyCaptor.firstValue["product_id"]).isEqualTo(PRODUCT_IDENTIFIER)
        assertThat(bodyCaptor.firstValue["purchase_token"]).isEqualTo(PURCHASE_TOKEN)
        assertThat(bodyCaptor.firstValue["site_id"]).isEqualTo(SITE_ID)
    }

    @Test
    fun `given app id, when create order, then correct header used`() = test {
        // GIVEN
        initResponse()

        // WHEN
        restClient.createOrder(
            productIdentifier = PRODUCT_IDENTIFIER,
            priceInCents = PRICE_IN_CENTS,
            currency = CURRENCY,
            purchaseToken = PURCHASE_TOKEN,
            appId = APP_ID,
            siteId = SITE_ID,
            customBaseUrl = null
        )

        // THEN
        assertThat(headersCaptor.firstValue["X-APP-ID"]).isEqualTo(APP_ID)
    }

    @Test
    fun `given successful response, when create order, then success returned`() = test {
        // GIVEN
        initResponse(data = CreateOrderResponseType(ORDER_ID))

        // WHEN
        val result = restClient.createOrder(
            productIdentifier = PRODUCT_IDENTIFIER,
            priceInCents = PRICE_IN_CENTS,
            currency = CURRENCY,
            purchaseToken = PURCHASE_TOKEN,
            appId = APP_ID,
            siteId = SITE_ID,
            customBaseUrl = null
        )

        // THEN
        assertThat(result).isInstanceOf(CreateOrderResponse.Success::class.java)
        assertThat((result as CreateOrderResponse.Success).orderId).isEqualTo(ORDER_ID)
    }

    @Test
    fun `given timeout error response, when create order, then timeout error returned`() = test {
        // GIVEN
        initResponse(
            error = WPComGsonNetworkError(
                BaseNetworkError(
                    BaseRequest.GenericErrorType.TIMEOUT,
                    VolleyError()
                )
            )
        )

        // WHEN
        val result = restClient.createOrder(
            productIdentifier = PRODUCT_IDENTIFIER,
            priceInCents = PRICE_IN_CENTS,
            currency = CURRENCY,
            purchaseToken = PURCHASE_TOKEN,
            appId = APP_ID,
            siteId = SITE_ID,
            customBaseUrl = null
        )

        // THEN
        assertThat((result as CreateOrderResponse.Error).type).isEqualTo(
            CreateOrderErrorType.TIMEOUT
        )
    }

    @Test
    fun `given api server error response, when create order, then api error returned`() = test {
        // GIVEN
        initResponse(
            error = WPComGsonNetworkError(
                BaseNetworkError(
                    BaseRequest.GenericErrorType.SERVER_ERROR,
                    VolleyError()
                )
            )
        )

        // WHEN
        val result = restClient.createOrder(
            productIdentifier = PRODUCT_IDENTIFIER,
            priceInCents = PRICE_IN_CENTS,
            currency = CURRENCY,
            purchaseToken = PURCHASE_TOKEN,
            appId = APP_ID,
            siteId = SITE_ID,
            customBaseUrl = null
        )

        // THEN
        assertThat((result as CreateOrderResponse.Error).type).isEqualTo(
            CreateOrderErrorType.API_ERROR
        )
    }

    @Test
    fun `given api auth error response, when create order, then auth error returned`() = test {
        // GIVEN
        initResponse(
            error = WPComGsonNetworkError(
                BaseNetworkError(
                    BaseRequest.GenericErrorType.AUTHORIZATION_REQUIRED,
                    VolleyError()
                )
            )
        )

        // WHEN
        val result = restClient.createOrder(
            productIdentifier = PRODUCT_IDENTIFIER,
            priceInCents = PRICE_IN_CENTS,
            currency = CURRENCY,
            purchaseToken = PURCHASE_TOKEN,
            appId = APP_ID,
            siteId = SITE_ID,
            customBaseUrl = null
        )

        // THEN
        assertThat((result as CreateOrderResponse.Error).type).isEqualTo(
            CreateOrderErrorType.AUTH_ERROR
        )
    }

    @Test
    fun `given api invalid error response, when create order, then invalid error returned`() =
        test {
            // GIVEN
            initResponse(
                error = WPComGsonNetworkError(
                    BaseNetworkError(
                        BaseRequest.GenericErrorType.INVALID_RESPONSE,
                        VolleyError()
                    )
                )
            )

            // WHEN
            val result = restClient.createOrder(
                productIdentifier = PRODUCT_IDENTIFIER,
                priceInCents = PRICE_IN_CENTS,
                currency = CURRENCY,
                purchaseToken = PURCHASE_TOKEN,
                appId = APP_ID,
                siteId = SITE_ID,
                customBaseUrl = null
            )

            // THEN
            assertThat((result as CreateOrderResponse.Error).type).isEqualTo(
                CreateOrderErrorType.INVALID_RESPONSE
            )
        }

    @Test
    fun `given generic error response, when create order, then generic error returned`() = test {
        // GIVEN
        initResponse(
            error = WPComGsonNetworkError(
                BaseNetworkError(
                    BaseRequest.GenericErrorType.UNKNOWN,
                    VolleyError()
                )
            )
        )

        // WHEN
        val result = restClient.createOrder(
            productIdentifier = PRODUCT_IDENTIFIER,
            priceInCents = PRICE_IN_CENTS,
            currency = CURRENCY,
            purchaseToken = PURCHASE_TOKEN,
            appId = APP_ID,
            siteId = SITE_ID,
            customBaseUrl = null
        )

        // THEN
        assertThat((result as CreateOrderResponse.Error).type).isEqualTo(
            CreateOrderErrorType.GENERIC_ERROR
        )
    }

    private suspend fun initResponse(
        data: CreateOrderResponseType? = null,
        error: WPComGsonNetworkError? = null
    ) {
        val nonNullData = data ?: mock()
        val response = if (error != null) {
            Response.Error(error)
        } else {
            Response.Success(nonNullData)
        }

        whenever(
            wpComGsonRequestBuilder.syncPostRequest(
                restClient = eq(restClient),
                url = urlCaptor.capture(),
                params = eq(null),
                body = bodyCaptor.capture(),
                clazz = eq(CreateOrderResponseType::class.java),
                retryPolicy = eq(null),
                headers = headersCaptor.capture()
            )
        ).thenReturn(response)
    }

    companion object {
        private const val PRODUCT_IDENTIFIER = "product_1"
        private const val PRICE_IN_CENTS = 100
        private const val CURRENCY = "USD"
        private const val PURCHASE_TOKEN = "purchase_token"
        private const val SITE_ID = 1L
        private const val APP_ID = "app_id"
        private const val ORDER_ID = 1L
    }
}
