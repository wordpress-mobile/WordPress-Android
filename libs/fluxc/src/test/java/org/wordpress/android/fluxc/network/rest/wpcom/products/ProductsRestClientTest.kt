package org.wordpress.android.fluxc.network.rest.wpcom.products

import com.android.volley.RequestQueue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.products.Product
import org.wordpress.android.fluxc.model.products.ProductsResponse
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class ProductsRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>

    private lateinit var productsRestClient: ProductsRestClient

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()

        productsRestClient = ProductsRestClient(
                dispatcher,
                wpComGsonRequestBuilder,
                null,
                requestQueue,
                accessToken,
                userAgent
        )
    }

    @Test
    fun `returns products on successful fetch`() = test {
        initRequest(data = Success(ProductsResponse(listOf(Product()))))

        val response = productsRestClient.fetchProducts()

        assertThat(response).isNotNull
        assertThat(response).isInstanceOf(Success::class.java)
        assertThat((response as Success).data.products).isNotEmpty
    }

    @Test
    fun `returns error on unsuccessful fetch`() = test {
        initRequest(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

        val response = productsRestClient.fetchProducts()

        assertThat(response).isNotNull
        assertThat(response).isNotInstanceOf(Success::class.java)
        assertThat(response).isInstanceOf(Error::class.java)
    }

    private suspend fun initRequest(
        data: Response<ProductsResponse>? = null,
        error: WPComGsonNetworkError? = null
    ) {
        val response = if (error != null) Error(error) else data

        whenever(
                wpComGsonRequestBuilder.syncGetRequest(
                        eq(productsRestClient),
                        urlCaptor.capture(),
                        paramsCaptor.capture(),
                        eq(ProductsResponse::class.java),
                        eq(false),
                        any(),
                        eq(false)
                )
        ).thenReturn(response)
    }
}
