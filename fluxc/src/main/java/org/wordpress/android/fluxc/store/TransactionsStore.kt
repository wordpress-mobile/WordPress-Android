package org.wordpress.android.fluxc.store

import android.text.TextUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.TransactionAction
import org.wordpress.android.fluxc.action.TransactionAction.CREATE_SHOPPING_CART
import org.wordpress.android.fluxc.action.TransactionAction.FETCH_SUPPORTED_COUNTRIES
import org.wordpress.android.fluxc.action.TransactionAction.REDEEM_CART_WITH_CREDITS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.DomainContactModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.SupportedDomainCountry
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient.CreateShoppingCartResponse
import org.wordpress.android.fluxc.store.TransactionsStore.FetchSupportedCountriesErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionsStore @Inject constructor(
    private val transactionsRestClient: TransactionsRestClient,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        when (action.type as? TransactionAction ?: return) {
            FETCH_SUPPORTED_COUNTRIES -> {
                coroutineEngine.launch(AppLog.T.API, this, "FETCH_SUPPORTED_COUNTRIES") {
                    emitChange(fetchSupportedCountries())
                }
            }
            CREATE_SHOPPING_CART -> {
                coroutineEngine.launch(AppLog.T.API, this, "CREATE_SHOPPING_CART") {
                    emitChange(createShoppingCart(action.payload as CreateShoppingCartPayload))
                }
            }
            REDEEM_CART_WITH_CREDITS -> {
                coroutineEngine.launch(AppLog.T.API, this, "REDEEM_CART_WITH_CREDITS") {
                    emitChange(redeemCartUsingCredits(action.payload as RedeemShoppingCartPayload))
                }
            }
        }
    }

    private suspend fun fetchSupportedCountries(): OnSupportedCountriesFetched {
        val supportedCountriesPayload = transactionsRestClient.fetchSupportedCountries()

        return if (!supportedCountriesPayload.isError) {
            // api returns couple of objects with empty names and codes so we need to filter them out
            val filteredCountries: Array<SupportedDomainCountry>? = supportedCountriesPayload.countries?.filter {
                !TextUtils.isEmpty(it.code) && !TextUtils.isEmpty(it.name)
            }?.toTypedArray()

            OnSupportedCountriesFetched(filteredCountries?.toMutableList())
        } else {
            OnSupportedCountriesFetched(
                    FetchSupportedCountriesError(
                            GENERIC_ERROR,
                            supportedCountriesPayload.error.message
                    )
            )
        }
    }

    private suspend fun createShoppingCart(payload: CreateShoppingCartPayload): OnShoppingCartCreated {
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
                    CreateShoppingCartError(CreateCartErrorType.GENERIC_ERROR, createdShoppingCartPayload.error.message)
            )
        }
    }

    private suspend fun redeemCartUsingCredits(payload: RedeemShoppingCartPayload): OnShoppingCartRedeemed {
        val cartRedeemedPayload = transactionsRestClient.redeemCartUsingCredits(
                payload.cartDetails,
                payload.domainContactModel
        )

        return if (!cartRedeemedPayload.isError) {
            OnShoppingCartRedeemed(cartRedeemedPayload.success)
        } else {
            OnShoppingCartRedeemed(cartRedeemedPayload.error)
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, TransactionsStore::class.java.simpleName + " onRegister")
    }

    // Actions

    data class OnSupportedCountriesFetched(
        val countries: List<SupportedDomainCountry>? = null
    ) : Store.OnChanged<FetchSupportedCountriesError>() {
        constructor(error: FetchSupportedCountriesError) : this() {
            this.error = error
        }
    }

    data class OnShoppingCartCreated(
        val cartDetails: CreateShoppingCartResponse? = null
    ) : Store.OnChanged<CreateShoppingCartError>() {
        constructor(error: CreateShoppingCartError) : this() {
            this.error = error
        }
    }

    data class OnShoppingCartRedeemed(val success: Boolean = false) : Store.OnChanged<RedeemShoppingCartError>() {
        constructor(error: RedeemShoppingCartError) : this() {
            this.error = error
        }
    }

    // Payloads

    class CreateShoppingCartPayload(
        val site: SiteModel,
        val productId: Int,
        val domainName: String,
        val isPrivacyEnabled: Boolean
    ) : Payload<BaseRequest.BaseNetworkError>()

    class RedeemShoppingCartPayload(
        val cartDetails: CreateShoppingCartResponse,
        val domainContactModel: DomainContactModel
    ) : Payload<BaseRequest.BaseNetworkError>()

    class FetchedSupportedCountriesPayload(
        val countries: Array<SupportedDomainCountry>? = null
    ) : Payload<BaseRequest.BaseNetworkError>()

    class CreatedShoppingCartPayload(
        val cartDetails: CreateShoppingCartResponse? = null
    ) : Payload<BaseRequest.BaseNetworkError>()

    class RedeemedShoppingCartPayload(
        val success: Boolean
    ) : Payload<RedeemShoppingCartError>()

    // Errors

    data class FetchSupportedCountriesError(val type: FetchSupportedCountriesErrorType, val message: String = "") :
            OnChangedError

    data class CreateShoppingCartError(var type: CreateCartErrorType, val message: String = "") : OnChangedError

    data class RedeemShoppingCartError(var type: TransactionErrorType, val message: String = "") : OnChangedError

    enum class FetchSupportedCountriesErrorType {
        GENERIC_ERROR
    }

    enum class CreateCartErrorType {
        GENERIC_ERROR
    }

    enum class TransactionErrorType {
        FIRST_NAME,
        LAST_NAME,
        ORGANIZATION,
        ADDRESS_1,
        ADDRESS_2,
        POSTAL_CODE,
        CITY,
        STATE,
        COUNTRY_CODE,
        EMAIL,
        PHONE,
        FAX,
        INSUFFICIENT_FUNDS,
        OTHER;

        companion object {
            fun fromString(string: String?): TransactionErrorType {
                if (string != null) {
                    for (v in values()) {
                        if (string.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return OTHER
            }
        }
    }
}
