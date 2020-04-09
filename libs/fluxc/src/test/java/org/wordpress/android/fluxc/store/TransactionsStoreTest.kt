package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.TransactionAction
import org.wordpress.android.fluxc.generated.TransactionActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.CREATE_SHOPPING_CART_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.DOMAIN_CONTACT_INFORMATION
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.SUPPORTED_COUNTRIES_MODEL
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient
import org.wordpress.android.fluxc.store.TransactionsStore.CreateShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.CreatedShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.FetchedSupportedCountriesPayload
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemedShoppingCartPayload
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class TransactionsStoreTest {
    @Mock private lateinit var transactionsRestClient: TransactionsRestClient
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var siteModel: SiteModel
    private lateinit var transactionsStore: TransactionsStore

    companion object {
        private const val TEST_DOMAIN_NAME = "superraredomainname156726.blog"
        private const val TEST_DOMAIN_PRODUCT_ID = 76
    }

    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        transactionsStore = TransactionsStore(transactionsRestClient, initCoroutineEngine(), dispatcher)
    }

    @Test
    fun fetchSupportedCountries() = test {
        whenever(transactionsRestClient.fetchSupportedCountries()).thenReturn(
                FetchedSupportedCountriesPayload(
                        SUPPORTED_COUNTRIES_MODEL
                )
        )
        val action = TransactionActionBuilder.generateNoPayloadAction(TransactionAction.FETCH_SUPPORTED_COUNTRIES)
        transactionsStore.onAction(action)

        verify(transactionsRestClient).fetchSupportedCountries()

        val expectedEvent = TransactionsStore.OnSupportedCountriesFetched(SUPPORTED_COUNTRIES_MODEL.toMutableList())
        verify(dispatcher).emitChange(eq(expectedEvent))
    }

    @Test
    fun createShoppingCart() = test {
        whenever(
                transactionsRestClient.createShoppingCart(
                        siteModel,
                        TEST_DOMAIN_PRODUCT_ID,
                        TEST_DOMAIN_NAME,
                        true
                )
        ).thenReturn(
                CreatedShoppingCartPayload(
                        CREATE_SHOPPING_CART_RESPONSE
                )
        )

        val payload = CreateShoppingCartPayload(
                siteModel,
                TEST_DOMAIN_PRODUCT_ID,
                TEST_DOMAIN_NAME, true
        )

        val action = TransactionActionBuilder.newCreateShoppingCartAction(payload)
        transactionsStore.onAction(action)

        verify(transactionsRestClient).createShoppingCart(
                payload.site,
                payload.productId,
                payload.domainName,
                payload.isPrivacyEnabled
        )

        val expectedEvent = TransactionsStore.OnShoppingCartCreated(CREATE_SHOPPING_CART_RESPONSE)
        verify(dispatcher).emitChange(eq(expectedEvent))
    }

    @Test
    fun redeemShoppingCartWithCredits() = test {
        whenever(
                transactionsRestClient.redeemCartUsingCredits(CREATE_SHOPPING_CART_RESPONSE, DOMAIN_CONTACT_INFORMATION)
        ).thenReturn(
                RedeemedShoppingCartPayload(true)
        )

        val payload = RedeemShoppingCartPayload(CREATE_SHOPPING_CART_RESPONSE, DOMAIN_CONTACT_INFORMATION)

        val action = TransactionActionBuilder.newRedeemCartWithCreditsAction(payload)
        transactionsStore.onAction(action)

        verify(transactionsRestClient).redeemCartUsingCredits(CREATE_SHOPPING_CART_RESPONSE, DOMAIN_CONTACT_INFORMATION)

        val expectedEvent = TransactionsStore.OnShoppingCartRedeemed(true)
        verify(dispatcher).emitChange(eq(expectedEvent))
    }
}
