package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.CartKey
import uniffi.wp_api.CreateShoppingCartParams
import uniffi.wp_api.CreateShoppingCartProduct
import uniffi.wp_api.CreateShoppingCartProductExtra
import uniffi.wp_api.ShoppingCart
import javax.inject.Inject

class CreateCartUseCase @Inject constructor(
    private val wpComApiClientProvider: WpComApiClientProvider,
    private val accountStore: AccountStore,
) {
    private var wpComApiClient: WpComApiClient? = null

    /**
     * Null when there is no WordPress.com account to make the request as.
     *
     * `AccountStore.accessToken` is typed nullable but reads `""` when signed
     * out, and is only null between an in-process sign out and the next
     * launch, so both have to be treated as no token.
     */
    @Synchronized
    private fun getOrCreateClient(): WpComApiClient? {
        val token = accountStore.accessToken?.takeIf { it.isNotEmpty() } ?: return null
        return wpComApiClient
            ?: wpComApiClientProvider.getWpComApiClient(token)
                .also { wpComApiClient = it }
    }

    /**
     * Creates a shopping cart holding a domain, and optionally a plan to bundle
     * it with.
     *
     * A null [site] carries the purchase for a site that does not exist yet.
     */
    suspend fun execute(
        site: SiteModel?,
        domainProductId: Int,
        domainName: String,
        isDomainPrivacyEnabled: Boolean,
        isTemporary: Boolean,
        planProductId: Int? = null
    ): CreateCartResult {
        val client = getOrCreateClient() ?: run {
            AppLog.e(
                AppLog.T.API,
                "Cannot create a shopping cart without a WP.com access token"
            )
            return CreateCartResult.Error()
        }
        val cartKey = cartKeyFor(site)
        val params = createShoppingCartParams(
            domainProductId = domainProductId,
            domainName = domainName,
            isDomainPrivacyEnabled = isDomainPrivacyEnabled,
            isTemporary = isTemporary,
            planProductId = planProductId
        )
        val result = client.request { it.shoppingCart().create(cartKey, params).data }
        return when (result) {
            is WpRequestResult.Success -> CreateCartResult.Success(result.response)
            else -> {
                AppLog.e(
                    AppLog.T.API,
                    "An error occurred while creating a shopping cart"
                )
                CreateCartResult.Error(result.apiErrorMessage(), result.isDeviceOffline())
            }
        }
    }
}

/**
 * Whose cart this is, which decides the endpoint the cart is created against.
 *
 * A null [site] means the domain is being bought before the site that will
 * carry it exists, which the API serves under `no-site`. The identifier is the
 * WordPress.com site ID, not the local one.
 */
internal fun cartKeyFor(site: SiteModel?): CartKey =
    site?.let { CartKey.Site(it.siteId.toULong()) } ?: CartKey.NoSite

/**
 * The cart request body: the domain being bought, and the plan it is bundled
 * with when there is one.
 *
 * Only the domain product carries [CreateShoppingCartProduct.meta] and
 * [CreateShoppingCartProduct.extra]; a plan is identified by its product ID
 * alone.
 */
internal fun createShoppingCartParams(
    domainProductId: Int,
    domainName: String,
    isDomainPrivacyEnabled: Boolean,
    isTemporary: Boolean,
    planProductId: Int?
): CreateShoppingCartParams {
    val domainProduct = CreateShoppingCartProduct(
        productId = domainProductId.toULong(),
        meta = domainName,
        extra = CreateShoppingCartProductExtra(privacy = isDomainPrivacyEnabled)
    )
    val planProduct = planProductId?.let { CreateShoppingCartProduct(productId = it.toULong()) }
    return CreateShoppingCartParams(
        temporary = isTemporary,
        products = listOfNotNull(domainProduct, planProduct)
    )
}

sealed interface CreateCartResult {
    data class Success(val cart: ShoppingCart) : CreateCartResult

    data class Error(
        val message: String? = null,
        val isDeviceOffline: Boolean = false,
    ) : CreateCartResult
}
