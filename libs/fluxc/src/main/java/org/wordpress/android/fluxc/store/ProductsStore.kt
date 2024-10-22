package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.ProductAction
import org.wordpress.android.fluxc.action.ProductAction.FETCH_PRODUCTS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.products.Product
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.products.ProductsRestClient
import org.wordpress.android.fluxc.store.ProductsStore.FetchProductsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductsStore @Inject constructor(
    private val productsRestClient: ProductsRestClient,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        when (action.type as? ProductAction ?: return) {
            FETCH_PRODUCTS -> {
                coroutineEngine.launch(T.API, this, "FETCH_PRODUCTS") {
                    emitChange(fetchProducts())
                }
            }
        }
    }

    override fun onRegister() {
        AppLog.d(T.API, ProductsStore::class.java.simpleName + " onRegister")
    }

    suspend fun fetchProducts(type: String? = null): OnProductsFetched =
        coroutineEngine.withDefaultContext(T.API, this, "Fetch products") {
            return@withDefaultContext when (val response = productsRestClient.fetchProducts(type)) {
                is Success -> {
                    OnProductsFetched(response.data.products)
                }
                is Error -> {
                    OnProductsFetched(FetchProductsError(GENERIC_ERROR, response.error.message))
                }
            }
        }

    data class OnProductsFetched(val products: List<Product>? = null) : OnChanged<FetchProductsError>() {
        constructor(error: FetchProductsError) : this() {
            this.error = error
        }
    }

    data class FetchProductsError(
        val type: FetchProductsErrorType,
        val message: String = ""
    ) : OnChangedError

    enum class FetchProductsErrorType {
        GENERIC_ERROR
    }
}
