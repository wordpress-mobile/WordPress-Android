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
import org.wordpress.android.fluxc.store.TransactionsStore.CreatedShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.FetchedSupportedCountriesPayload
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemShoppingCartError
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemedShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType
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
        const val domainCreditPaymentMethod = "WPCOM_Billing_WPCOM"
    }

    suspend fun fetchSupportedCountries(): FetchedSupportedCountriesPayload {
        val url = WPCOMREST.me.transactions.supported_countries.urlV1_1

        return when (val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                emptyMap(),
                Array<SupportedDomainCountry>::class.java
        )) {
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
        productId: Int,
        domainName: String,
        isPrivacyProtectionEnabled: Boolean
    ): CreatedShoppingCartPayload {
        val url = WPCOMREST.me.shopping_cart.site(site.siteId).urlV1_1

        val domainProduct = mapOf(
                "product_id" to productId,
                "meta" to domainName,
                "extra" to PrivacyExtra(isPrivacyProtectionEnabled)
        )

        val params = mapOf(
                "temporary" to "true",
                "products" to arrayOf(domainProduct)
        )

        return when (val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                params,
                CreateShoppingCartResponse::class.java
        )) {
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
        cartResponse: CreateShoppingCartResponse,
        domainContactInformation: DomainContactModel
    ): RedeemedShoppingCartPayload {
        val url = WPCOMREST.me.transactions.urlV1_1

        val paymentMethod = mapOf(
                "payment_method" to domainCreditPaymentMethod
        )

        val params = mapOf(
                "domain_details" to domainContactInformation,
                "cart" to cartResponse,
                "payment" to paymentMethod
        )

        return when (val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                params,
                CreateShoppingCartResponse::class.java
        )) {
            is Success -> {
                RedeemedShoppingCartPayload(true)
            }
            is WPComGsonRequestBuilder.Response.Error -> {
                val payload = RedeemedShoppingCartPayload(false)
                payload.error = RedeemShoppingCartError(
                        TransactionErrorType.fromString(response.error.apiError),
                        response.error.message
                )
                payload
            }
        }
    }

    private data class PrivacyExtra(val privacy: Boolean)

    data class CreateShoppingCartResponse(
        val blog_id: Int,
        val cart_key: String?,
        val products: List<Product>?
    ) : Response {
        data class Product(
            val product_id: Int,
            val meta: String?
        )
    }
}
