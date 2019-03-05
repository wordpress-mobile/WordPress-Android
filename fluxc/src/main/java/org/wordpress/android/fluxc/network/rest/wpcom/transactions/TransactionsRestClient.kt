package org.wordpress.android.fluxc.network.rest.wpcom.transactions

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.DomainContactModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.PlanOffersRestClient.PlanOffersResponse.Feature
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.fluxc.store.TransactionsStore.CreatedShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.FetchedSupportedCountriesPayload
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemedShoppingCartPayload
import javax.inject.Singleton

@Singleton
class TransactionsRestClient
constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    companion object {
        const val privateRegistrationProductID = 16
        const val freeDomainPaymentMethod = "WPCOM_Billing_WPCOM"
    }

    suspend fun fetchSupportedCountries(): FetchedSupportedCountriesPayload {
        val url = WPCOMREST.me.transactions.supported_countries.urlV1_1

        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                emptyMap(),
                Array<Country>::class.java,
                enableCaching = false,
                forced = true
        )
        return when (response) {
            is Success -> {
                FetchedSupportedCountriesPayload(response.data)
            }
            is WPComGsonRequestBuilder.Response.Error -> {
                val payload = FetchedSupportedCountriesPayload()
                payload.error = response.error
                payload
            }
        }
    }

    suspend fun createShoppingCart(
        site: SiteModel,
        domainSuggestion: DomainSuggestionResponse,
        isPrivacyProtectionEnabled: Boolean
    ): CreatedShoppingCartPayload {
        val url = WPCOMREST.me.shopping_cart.site(site.siteId).urlV1_1

//        val params = HashMap<String, Any>()
//
//        val products = JsonArray()
//
//        val mainProduct = JsonObject()
//        mainProduct.addProperty("product_id", domainSuggeStion.product_id)
//        mainProduct.addProperty("meta", domainSuggeStion.domain_name)
//
//        val privacyProtectionProduct = JsonObject()
//        privacyProtectionProduct.addProperty("product_id", privateRegistrationProductID)
//        privacyProtectionProduct.addProperty("meta", domainSuggeStion.domain_name)
//
//        products.add(mainProduct)
//
//        if (isPrivacyProtectionEnabled) {
//            products.add(privacyProtectionProduct)
//        }
//
//        params["temporary"] = "true"
//        params["products"] = products

        val mainProduct = mapOf(
                "product_id" to domainSuggestion.product_id,
                "meta" to domainSuggestion.domain_name
        )

        val privacyProtectionProduct = mapOf(
                "product_id" to privateRegistrationProductID,
                "meta" to domainSuggestion.domain_name
        )

        val products = if (isPrivacyProtectionEnabled) arrayOf(mainProduct, privacyProtectionProduct) else arrayOf(
                mainProduct
        )

        val params = mapOf(
                "temporary" to "true",
                "products" to products
        )

        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                params,
                CartResponse::class.java
        )
        return when (response) {
            is Success -> {
                CreatedShoppingCartPayload(response.data)
            }
            is WPComGsonRequestBuilder.Response.Error -> {
                val payload = CreatedShoppingCartPayload()
                payload.error = response.error
                payload
            }
        }
    }

    suspend fun redeemCartUsingCredits(
        cartResponse: CartResponse,
        domainContactInformation: DomainContactModel
    ): RedeemedShoppingCartPayload {
        val url = WPCOMREST.me.transactions.urlV1_1

        val paymentMethod = mapOf(
                "payment_method" to freeDomainPaymentMethod
        )

        val params = mapOf(
                "domain_details" to domainContactInformation,
                "cart" to cartResponse,
                "payment" to paymentMethod
        )

        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                params,
                CartResponse::class.java
        )
        return when (response) {
            is Success -> {
                RedeemedShoppingCartPayload(true)
            }
            is WPComGsonRequestBuilder.Response.Error -> {
                val payload = RedeemedShoppingCartPayload(false)
                payload.error = response.error
                payload
            }
        }
    }

    data class CartResponse(
        val blog_id: Int,
        val cart_key: String?,
        val products: List<Feature>?
    ) : Response {
        data class Product(
            val product_id: Int,
            val meta: String?
        )
    }
}
