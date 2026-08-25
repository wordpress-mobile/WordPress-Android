package org.wordpress.android.ui.domains

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.ui.domains.usecases.FetchPlansUseCase
import org.wordpress.android.ui.domains.usecases.SitePlansResult
import org.wordpress.android.ui.domains.usecases.hasDomainCredit
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.SitePlan
import uniffi.wp_api.SitePlanCurrentPlanInfo

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FetchPlansUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var wpComApiClientProvider: WpComApiClientProvider

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var wpComApiClient: WpComApiClient

    private lateinit var useCase: FetchPlansUseCase

    private val site = SiteModel().apply { siteId = 1234L }

    @Before
    fun setUp() {
        whenever(accountStore.accessToken).thenReturn("test-token")
        whenever(wpComApiClientProvider.getWpComApiClient("test-token"))
            .thenReturn(wpComApiClient)
        useCase = FetchPlansUseCase(wpComApiClientProvider, accountStore)
    }

    @Test
    fun `given plans are returned, when execute, returns success`() = test {
        stubPlans(mapOf(FREE_PLAN to currentPlan(hasDomainCredit = false)))

        val result = useCase.execute(site)

        assertThat(result).isInstanceOf(SitePlansResult.Success::class.java)
        assertThat((result as SitePlansResult.Success).plans).hasSize(1)
    }

    @Test
    fun `given plans returns error, when execute, returns error`() = test {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(
                WpRequestResult.UnknownError<Any>(
                    500.toUInt(),
                    "Internal Server Error",
                    "",
                    RequestMethod.GET
                )
            )

        val result = useCase.execute(site)

        assertThat(result).isInstanceOf(SitePlansResult.Error::class.java)
    }

    @Test
    fun `given no access token, when execute, returns error`() = test {
        whenever(accountStore.accessToken).thenReturn(null)

        val result = useCase.execute(site)

        assertThat(result).isInstanceOf(SitePlansResult.Error::class.java)
    }

    @Test
    fun `given the current plan carries a credit, hasDomainCredit is true`() = test {
        stubPlans(
            mapOf(
                FREE_PLAN to nonCurrentPlan(),
                PREMIUM_PLAN to currentPlan(hasDomainCredit = true),
            )
        )

        assertThat(useCase.execute(site).hasDomainCredit()).isTrue()
    }

    @Test
    fun `given the current plan carries no credit, hasDomainCredit is false`() = test {
        stubPlans(
            mapOf(
                FREE_PLAN to nonCurrentPlan(),
                PREMIUM_PLAN to currentPlan(hasDomainCredit = false),
            )
        )

        assertThat(useCase.execute(site).hasDomainCredit()).isFalse()
    }

    @Test
    fun `given no plan is current, hasDomainCredit is false`() = test {
        stubPlans(mapOf(FREE_PLAN to nonCurrentPlan(), PREMIUM_PLAN to nonCurrentPlan()))

        assertThat(useCase.execute(site).hasDomainCredit()).isFalse()
    }

    /**
     * The response is a map and its iteration order is not stable, so a credit
     * has to be found wherever it sits rather than on whichever plan comes out
     * of the map first.
     */
    @Test
    fun `given two plans carry the credit group, the one with the credit decides`() = test {
        stubPlans(
            mapOf(
                FREE_PLAN to currentPlan(hasDomainCredit = false),
                PREMIUM_PLAN to currentPlan(hasDomainCredit = true),
            )
        )

        assertThat(useCase.execute(site).hasDomainCredit()).isTrue()
    }

    @Test
    fun `given a blank access token, when execute, returns error`() = test {
        whenever(accountStore.accessToken).thenReturn("")

        val result = useCase.execute(site)

        assertThat(result).isInstanceOf(SitePlansResult.Error::class.java)
    }

    @Test
    fun `given the fetch failed, hasDomainCredit is false`() {
        assertThat(SitePlansResult.Error.hasDomainCredit()).isFalse()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun stubPlans(plans: Map<ULong, SitePlan>) {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(WpRequestResult.Success(plans) as WpRequestResult<Any>)
    }

    private fun currentPlan(hasDomainCredit: Boolean): SitePlan = mock {
        on { currentPlan } doReturn SitePlanCurrentPlanInfo(
            purchaseId = null,
            userIsOwner = null,
            hasDomainCredit = hasDomainCredit
        )
    }

    private fun nonCurrentPlan(): SitePlan = mock {
        on { currentPlan } doReturn null
    }

    companion object {
        private const val FREE_PLAN = 1uL
        private const val PREMIUM_PLAN = 1003uL
    }
}
