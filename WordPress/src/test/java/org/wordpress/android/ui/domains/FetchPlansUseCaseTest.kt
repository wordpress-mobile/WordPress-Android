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
import org.wordpress.android.ui.domains.usecases.FetchPlansUseCase
import org.wordpress.android.ui.domains.usecases.SitePlansResult
import org.wordpress.android.ui.domains.usecases.hasDomainCredit
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.SitePlan

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FetchPlansUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var wpComApiClient: WpComApiClient

    private lateinit var useCase: FetchPlansUseCase

    private val site = SiteModel().apply { siteId = 1234L }

    @Before
    fun setUp() {
        useCase = FetchPlansUseCase(wpComApiClient)
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
    fun `given the current plan carries a credit, hasDomainCredit is true`() = test {
        stubPlans(
            mapOf(
                FREE_PLAN to nonCurrentPlan(),
                PREMIUM_PLAN to currentPlan(hasDomainCredit = true),
            )
        )

        assertThat(fetchedCredit()).isTrue()
    }

    @Test
    fun `given the current plan carries no credit, hasDomainCredit is false`() = test {
        stubPlans(
            mapOf(
                FREE_PLAN to nonCurrentPlan(),
                PREMIUM_PLAN to currentPlan(hasDomainCredit = false),
            )
        )

        assertThat(fetchedCredit()).isFalse()
    }

    @Test
    fun `given no plan is current, hasDomainCredit is false`() = test {
        stubPlans(mapOf(FREE_PLAN to nonCurrentPlan(), PREMIUM_PLAN to nonCurrentPlan()))

        assertThat(fetchedCredit()).isFalse()
    }

    /**
     * A site is expected to have one current plan. This pins the deliberate
     * choice for the case where it does not, which is to report the credit
     * rather than hide it.
     */
    @Test
    fun `given more than one plan reports itself current, a credit on either counts`() = test {
        stubPlans(
            mapOf(
                FREE_PLAN to currentPlan(hasDomainCredit = false),
                PREMIUM_PLAN to currentPlan(hasDomainCredit = true),
            )
        )

        assertThat(fetchedCredit()).isTrue()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun stubPlans(plans: Map<ULong, SitePlan>) {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(WpRequestResult.Success(plans) as WpRequestResult<Any>)
    }

    private suspend fun fetchedCredit(): Boolean =
        (useCase.execute(site) as SitePlansResult.Success).hasDomainCredit()

    private fun currentPlan(hasDomainCredit: Boolean): SitePlan =
        testSitePlan(currentPlan = testCurrentPlan(hasDomainCredit))

    private fun nonCurrentPlan(): SitePlan = testSitePlan()

    companion object {
        private const val FREE_PLAN = 1uL
        private const val PREMIUM_PLAN = 1003uL
    }
}
