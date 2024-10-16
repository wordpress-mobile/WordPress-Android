package org.wordpress.android.fluxc.store

import com.android.volley.VolleyError
import junit.framework.Assert.assertTrue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.ProductAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.products.Product
import org.wordpress.android.fluxc.model.products.ProductsResponse
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.products.ProductsRestClient
import org.wordpress.android.fluxc.store.ProductsStore.OnProductsFetched
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class ProductsStoreTest {
    @Mock private lateinit var productsRestClient: ProductsRestClient
    @Mock private lateinit var dispatcher: Dispatcher

    private lateinit var productsStore: ProductsStore

    @Before
    fun setUp() {
        productsStore = ProductsStore(productsRestClient, initCoroutineEngine(), dispatcher)
    }

    @Test
    fun fetchProductsAction() = test {
        initRestClient(data = Success(ProductsResponse(listOf(Product()))))

        productsStore.onAction(Action(ProductAction.FETCH_PRODUCTS, null))

        verify(productsRestClient).fetchProducts()
    }

    @Test
    fun fetchProductsSuccess() = test {
        initRestClient(data = Success(ProductsResponse(listOf(Product()))))

        val response = productsStore.fetchProducts()

        verify(productsRestClient).fetchProducts()
        assertThat(response).isInstanceOf(OnProductsFetched::class.java)
        assertThat(response.products).isNotEmpty
    }

    @Test
    fun fetchProductsFail() = test {
        initRestClient(Error(WPComGsonNetworkError(
                BaseNetworkError(NETWORK_ERROR, "error", VolleyError("")))))

        val response = productsStore.fetchProducts()

        verify(productsRestClient).fetchProducts()
        assertThat(response.products).isNull()
        assertTrue(response.isError)
        assertThat(response.error.message).isEqualTo("error")
    }

    private suspend fun initRestClient(
        data: Response<ProductsResponse>? = null,
        error: WPComGsonNetworkError? = null
    ) {
        val response = if (error != null) Error(error) else data

        whenever(productsRestClient.fetchProducts()).thenReturn(response)
    }
}
