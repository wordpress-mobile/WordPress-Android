package org.wordpress.android.ui.domains

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.ui.domains.usecases.CreateCartResult
import org.wordpress.android.ui.domains.usecases.CreateCartUseCase
import org.wordpress.android.ui.domains.usecases.cartKeyFor
import org.wordpress.android.ui.domains.usecases.createShoppingCartParams
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.CartKey
import uniffi.wp_api.RequestMethod

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CreateCartUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var wpComApiClientProvider: WpComApiClientProvider

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var wpComApiClient: WpComApiClient

    private lateinit var useCase: CreateCartUseCase

    private val site = SiteModel().apply { siteId = 1234L }

    @Before
    fun setUp() {
        whenever(accountStore.accessToken).thenReturn("test-token")
        whenever(wpComApiClientProvider.getWpComApiClient("test-token"))
            .thenReturn(wpComApiClient)
        useCase = CreateCartUseCase(wpComApiClientProvider, accountStore)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `given the cart is created, when execute, returns success`() = test {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(WpRequestResult.Success(Unit) as WpRequestResult<Any>)

        val result = useCase.execute(site, PRODUCT_ID, DOMAIN_NAME, true, false)

        assertThat(result).isEqualTo(CreateCartResult.Success)
    }

    @Test
    fun `given cart creation returns error, when execute, returns error`() = test {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(
                WpRequestResult.UnknownError<Any>(
                    500.toUInt(),
                    "Internal Server Error",
                    "",
                    RequestMethod.POST
                )
            )

        val result = useCase.execute(site, PRODUCT_ID, DOMAIN_NAME, true, false)

        assertThat(result).isEqualTo(CreateCartResult.Error)
    }

    @Test
    fun `given no access token, when execute, returns error`() = test {
        whenever(accountStore.accessToken).thenReturn(null)

        val result = useCase.execute(site, PRODUCT_ID, DOMAIN_NAME, true, false)

        assertThat(result).isEqualTo(CreateCartResult.Error)
    }

    @Test
    fun `given a blank access token, when execute, returns error`() = test {
        whenever(accountStore.accessToken).thenReturn("")

        val result = useCase.execute(site, PRODUCT_ID, DOMAIN_NAME, true, false)

        assertThat(result).isEqualTo(CreateCartResult.Error)
    }

    @Test
    fun `given a site, the cart key is its WordPress-com site ID`() {
        assertThat(cartKeyFor(site)).isEqualTo(CartKey.Site(1234UL))
    }

    @Test
    fun `given no site, the cart key is no-site`() {
        assertThat(cartKeyFor(null)).isEqualTo(CartKey.NoSite)
    }

    @Test
    fun `given no plan, the cart holds the domain alone`() {
        val params = createShoppingCartParams(
            domainProductId = PRODUCT_ID,
            domainName = DOMAIN_NAME,
            isDomainPrivacyEnabled = true,
            isTemporary = false,
            planProductId = null
        )

        assertThat(params.temporary).isFalse()
        assertThat(params.products).hasSize(1)
        with(params.products.first()) {
            assertThat(productId).isEqualTo(PRODUCT_ID.toULong())
            assertThat(meta).isEqualTo(DOMAIN_NAME)
            assertThat(extra?.privacy).isTrue()
        }
    }

    @Test
    fun `given privacy is off, the domain product says so`() {
        val params = createShoppingCartParams(
            domainProductId = PRODUCT_ID,
            domainName = DOMAIN_NAME,
            isDomainPrivacyEnabled = false,
            isTemporary = false,
            planProductId = null
        )

        assertThat(params.products.first().extra?.privacy).isFalse()
    }

    @Test
    fun `given a plan, it follows the domain and carries its product ID alone`() {
        val params = createShoppingCartParams(
            domainProductId = PRODUCT_ID,
            domainName = DOMAIN_NAME,
            isDomainPrivacyEnabled = true,
            isTemporary = true,
            planProductId = PLAN_PRODUCT_ID
        )

        assertThat(params.temporary).isTrue()
        assertThat(params.products).hasSize(2)
        with(params.products[1]) {
            assertThat(productId).isEqualTo(PLAN_PRODUCT_ID.toULong())
            assertThat(meta).isNull()
            assertThat(extra).isNull()
        }
    }

    companion object {
        private const val PRODUCT_ID = 6
        private const val PLAN_PRODUCT_ID = 1009
        private const val DOMAIN_NAME = "domainname.com"
    }
}
