package org.wordpress.android.fluxc.store

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.TransactionAction
import org.wordpress.android.fluxc.action.TransactionAction.CREATE_SHOPPING_CART
import org.wordpress.android.fluxc.action.TransactionAction.GET_SUPPORTED_COUNTRIES
import org.wordpress.android.fluxc.action.TransactionAction.REDEEM_CART_WITH_CREDITS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.DomainContactModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.Country
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient.CartResponse
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.GENERIC_ERROR
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class TransactionsStore @Inject constructor(
    private val transactionsRestClient: TransactionsRestClient,
    private val coroutineContext: CoroutineContext,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? TransactionAction ?: return
        when (actionType) {
            GET_SUPPORTED_COUNTRIES -> {
                GlobalScope.launch(coroutineContext) {
                    emitChange(fetchSupportedCountries())
                }
            }
            CREATE_SHOPPING_CART -> {
                GlobalScope.launch(coroutineContext) {
                    emitChange(createShoppingCard(action.payload as CreateShoppingCartPayload))
                }
            }
            REDEEM_CART_WITH_CREDITS -> {
                GlobalScope.launch(coroutineContext) {
                    emitChange(redeemCardUsingCredits(action.payload as RedeemShoppingCartPayload))
                }
            }
        }
    }

    private suspend fun fetchSupportedCountries(): OnSupportedCountriesFetched {
        val supportedCountriesPayload = transactionsRestClient.fetchSupportedCountries()

        return if (!supportedCountriesPayload.isError) {
            OnSupportedCountriesFetched(supportedCountriesPayload.countries)
        } else {
            OnSupportedCountriesFetched(
                    FetchSupportedCountriesError(
                            TransactionErrorType.GENERIC_ERROR,
                            supportedCountriesPayload.error.message
                    )
            )
        }
    }

    private suspend fun createShoppingCard(payload: CreateShoppingCartPayload): OnShoppingCartCreated {
        val createdShoppingCartPayload = transactionsRestClient.createShoppingCart(
                payload.site,
                payload.productId,
                payload.domainName,
                payload.isPrivacyEnabled
        )

        return if (!createdShoppingCartPayload.isError) {
            OnShoppingCartCreated(createdShoppingCartPayload.cartDetails)
        } else {
            OnShoppingCartCreated(
                    CreateShoppingCartError(GENERIC_ERROR, createdShoppingCartPayload.error.message)
            )
        }
    }

    private suspend fun redeemCardUsingCredits(payload: RedeemShoppingCartPayload): OnCartRedeemed {
        val cartRedeemedPayload = transactionsRestClient.redeemCartUsingCredits(
                payload.cartDetails,
                payload.domainContactModel
        )

        return if (!cartRedeemedPayload.isError) {
            OnCartRedeemed(cartRedeemedPayload.success)
        } else {
            OnCartRedeemed(
                    RedeemShoppingCartError(GENERIC_ERROR, cartRedeemedPayload.error.message)
            )
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, TransactionsStore::class.java.simpleName + " onRegister")
    }

    // Actions

    data class OnSupportedCountriesFetched(
        val countries: Array<Country>? = null
    ) : Store.OnChanged<FetchSupportedCountriesError>() {
        constructor(error: FetchSupportedCountriesError) : this() {
            this.error = error
        }
    }

    data class OnShoppingCartCreated(
        val cartDetails: CartResponse? = null
    ) : Store.OnChanged<CreateShoppingCartError>() {
        constructor(error: CreateShoppingCartError) : this() {
            this.error = error
        }
    }

    data class OnCartRedeemed(val success: Boolean = false) : Store.OnChanged<RedeemShoppingCartError>() {
        constructor(error: RedeemShoppingCartError) : this() {
            this.error = error
        }
    }

    // Payloads

    class CreateShoppingCartPayload(
        val site: SiteModel,
        val productId: String,
        val domainName: String,
        val isPrivacyEnabled: Boolean
    ) : Payload<BaseRequest.BaseNetworkError>()

    class RedeemShoppingCartPayload(
        val cartDetails: CartResponse,
        val domainContactModel: DomainContactModel
    ) : Payload<BaseRequest.BaseNetworkError>()

    class FetchedSupportedCountriesPayload(
        val countries: Array<Country>? = null
    ) : Payload<BaseRequest.BaseNetworkError>()

    class CreatedShoppingCartPayload(
        val cartDetails: CartResponse? = null
    ) : Payload<BaseRequest.BaseNetworkError>()

    class RedeemedShoppingCartPayload(
        val success: Boolean
    ) : Payload<BaseRequest.BaseNetworkError>()

    // Errors

    data class FetchSupportedCountriesError(val type: TransactionErrorType, val message: String = "") : OnChangedError

    data class CreateShoppingCartError(var type: TransactionErrorType, val message: String = "") : OnChangedError

    data class RedeemShoppingCartError(var type: TransactionErrorType, val message: String = "") : OnChangedError

    enum class TransactionErrorType {
        GENERIC_ERROR
    }
}
