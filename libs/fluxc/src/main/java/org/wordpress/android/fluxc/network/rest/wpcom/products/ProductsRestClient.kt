package org.wordpress.android.fluxc.network.rest.wpcom.products

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.products.ProductsResponse
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ProductsRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchProducts(type: String? = null): Response<ProductsResponse> {
        val url = WPCOMREST.products.urlV1_1
        return wpComGsonRequestBuilder.syncGetRequest(
            this,
            url,
            mutableMapOf<String, String>().apply {
                type?.let {
                    put("type", it)
                }
            },
            ProductsResponse::class.java
        )
    }
}
