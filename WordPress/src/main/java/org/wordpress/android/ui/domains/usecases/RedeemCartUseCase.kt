package org.wordpress.android.ui.domains.usecases

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.DomainContactInformation
import uniffi.wp_api.RedeemCartParams
import uniffi.wp_api.ShoppingCart
import uniffi.wp_api.TransactionPayment
import uniffi.wp_api.TransactionPaymentMethod
import javax.inject.Inject

class RedeemCartUseCase @Inject constructor(
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
     * Pays for [cart] with the account's WordPress.com credits, registering the
     * domain it holds to [contact].
     *
     * This spends a credit and cannot be undone.
     */
    suspend fun execute(
        cart: ShoppingCart,
        contact: DomainContactInformation
    ): RedeemCartResult {
        val client = getOrCreateClient() ?: run {
            AppLog.e(
                AppLog.T.API,
                "Cannot redeem a shopping cart without a WP.com access token"
            )
            return RedeemCartResult.Error()
        }
        val params = redeemCartParams(cart, contact)
        val result = client.request { it.me().redeemCart(params).data }
        return when (result) {
            is WpRequestResult.Success ->
                if (result.response.success) {
                    RedeemCartResult.Success
                } else {
                    AppLog.e(
                        AppLog.T.API,
                        "A shopping cart was charged for but ${result.response.failedPurchases.size} " +
                                "of its sites had products that could not be provisioned"
                    )
                    RedeemCartResult.PartialFailure
                }
            else -> {
                AppLog.e(
                    AppLog.T.API,
                    "An error occurred while redeeming a shopping cart"
                )
                RedeemCartResult.Error(
                    field = DomainContactField.fromApiErrorCode(result.apiErrorCode()),
                    message = result.apiErrorMessage(),
                    isDeviceOffline = result.isDeviceOffline(),
                )
            }
        }
    }
}

/**
 * The redeem request body.
 *
 * [cart] is handed back whole rather than rebuilt from its parts. The server
 * reads the tax location out of it and has no fallback, so a cart that lost
 * fields on the way is taxed differently from the one the customer was quoted.
 */
internal fun redeemCartParams(
    cart: ShoppingCart,
    contact: DomainContactInformation
) = RedeemCartParams(
    cart = cart,
    payment = TransactionPayment(paymentMethod = TransactionPaymentMethod.USE_CREDITS),
    domainDetails = contact,
)

sealed interface RedeemCartResult {
    data object Success : RedeemCartResult

    /**
     * The transaction was charged and a receipt exists, but at least one
     * product in the cart could not be provisioned. A rejection that costs the
     * customer nothing arrives as an [Error] instead.
     */
    data object PartialFailure : RedeemCartResult

    data class Error(
        val field: DomainContactField? = null,
        val message: String? = null,
        val isDeviceOffline: Boolean = false,
    ) : RedeemCartResult
}

/**
 * A contact field the server rejected, so the form can mark it.
 *
 * A validation failure comes back as an error code naming the offending field.
 * Codes that name nothing on this form — `fax`, `insufficient_funds` — have no
 * entry here, and the screen reports those through the message alone.
 */
enum class DomainContactField(val apiErrorCode: String) {
    FIRST_NAME("first_name"),
    LAST_NAME("last_name"),
    ORGANIZATION("organization"),
    ADDRESS_1("address_1"),
    ADDRESS_2("address_2"),
    POSTAL_CODE("postal_code"),
    CITY("city"),
    STATE("state"),
    COUNTRY_CODE("country_code"),
    EMAIL("email"),
    PHONE("phone");

    companion object {
        fun fromApiErrorCode(code: String?): DomainContactField? =
            code?.let { reported -> entries.firstOrNull { it.apiErrorCode.equals(reported, ignoreCase = true) } }
    }
}
