package org.wordpress.android.ui.mysite.cards.connectivity

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.repositories.SiteAuthState
import org.wordpress.android.repositories.SiteProvisioningSource
import org.wordpress.android.repositories.SiteReadiness
import org.wordpress.android.ui.mysite.MySiteCardAndItem

private const val TEST_SITE_LOCAL_ID = 42

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteConnectivityBannerViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var siteProvisioningSource: SiteProvisioningSource

    private lateinit var siteTest: SiteModel
    private lateinit var slice: SiteConnectivityBannerViewModelSlice
    private val emittedBanners = mutableListOf<MySiteCardAndItem?>()

    @Before
    fun setUp() {
        siteTest = SiteModel().apply { id = TEST_SITE_LOCAL_ID }
        slice = SiteConnectivityBannerViewModelSlice(siteProvisioningSource)
        slice.initialize(testScope())
        slice.uiModel.observeForever { emittedBanners.add(it) }
    }

    private fun stubReadiness(
        site: SiteModel,
        readiness: SiteReadiness,
    ): MutableStateFlow<SiteReadiness> {
        val flow = MutableStateFlow(readiness)
        whenever(siteProvisioningSource.stateFor(site)).thenReturn(flow)
        return flow
    }

    @Test
    fun `given unreachable, when fetchCapabilities invoked, then banner is shown`() = test {
        stubReadiness(siteTest, SiteReadiness.Unreachable)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()

        val banner = emittedBanners.last() as MySiteCardAndItem.Item.SingleActionCard
        assertThat(banner.textResource).isEqualTo(R.string.site_connectivity_banner_text)
        assertThat(banner.showLearnMore).isFalse
    }

    @Test
    fun `given ready, when fetchCapabilities invoked, then banner is null`() = test {
        stubReadiness(siteTest, SiteReadiness.Ready)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()

        assertThat(emittedBanners.last()).isNull()
    }

    @Test
    fun `given needs auth, when fetchCapabilities invoked, then banner is null`() = test {
        // Credentials are the problem — the application-password card owns it, banner stays hidden.
        stubReadiness(siteTest, SiteReadiness.NeedsAuth(SiteAuthState.Unprovisionable(hadCredentials = false)))

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()

        assertThat(emittedBanners.last()).isNull()
    }

    @Test
    fun `given probing, when fetchCapabilities invoked, then banner is null`() = test {
        stubReadiness(siteTest, SiteReadiness.Probing)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()

        assertThat(emittedBanners.last()).isNull()
    }

    @Test
    fun `given transient error, when fetchCapabilities invoked, then banner is null`() = test {
        stubReadiness(siteTest, SiteReadiness.TransientError)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()

        assertThat(emittedBanners.last()).isNull()
    }

    @Test
    fun `given unreachable then recovered to ready, when state changes, then banner clears`() = test {
        val flow = stubReadiness(siteTest, SiteReadiness.Unreachable)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()
        assertThat(emittedBanners.last()).isNotNull

        flow.value = SiteReadiness.Ready
        advanceUntilIdle()

        assertThat(emittedBanners.last()).isNull()
    }

    @Test
    fun `given user-initiated, when fetchCapabilities invoked, then source is invalidated`() = test {
        stubReadiness(siteTest, SiteReadiness.Ready)

        slice.fetchCapabilities(siteTest, isUserInitiated = true)
        advanceUntilIdle()

        verify(siteProvisioningSource).invalidate(siteTest)
    }

    @Test
    fun `given non-user-initiated, when fetchCapabilities invoked, then source is not invalidated`() = test {
        stubReadiness(siteTest, SiteReadiness.Ready)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()

        verify(siteProvisioningSource, never()).invalidate(any())
    }

    @Test
    fun `given banner showing, when retry tapped, then source is invalidated`() = test {
        stubReadiness(siteTest, SiteReadiness.Unreachable)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()
        val banner = emittedBanners.last() as MySiteCardAndItem.Item.SingleActionCard

        banner.onActionClick()
        advanceUntilIdle()

        verify(siteProvisioningSource).invalidate(siteTest)
    }

    @Test
    fun `when clearBanner invoked, then banner is null`() = test {
        stubReadiness(siteTest, SiteReadiness.Unreachable)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()
        assertThat(emittedBanners.last()).isNotNull
        slice.clearBanner()
        advanceUntilIdle()

        assertThat(emittedBanners.last()).isNull()
    }

    @Test
    fun `given banner cleared, when retry tapped, then no invalidate runs`() = test {
        stubReadiness(siteTest, SiteReadiness.Unreachable)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()
        val banner = emittedBanners.last() as MySiteCardAndItem.Item.SingleActionCard
        slice.clearBanner()
        advanceUntilIdle()

        banner.onActionClick()
        advanceUntilIdle()

        verify(siteProvisioningSource, never()).invalidate(any())
    }

    @Test
    fun `given site switched, when old site becomes unreachable, then banner ignores it`() = test {
        val siteB = SiteModel().apply { id = TEST_SITE_LOCAL_ID + 1 }
        val flowA = stubReadiness(siteTest, SiteReadiness.Ready)
        stubReadiness(siteB, SiteReadiness.Ready)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()
        slice.fetchCapabilities(siteB, isUserInitiated = false)
        advanceUntilIdle()

        flowA.value = SiteReadiness.Unreachable
        advanceUntilIdle()

        assertThat(
            emittedBanners.filterIsInstance<MySiteCardAndItem.Item.SingleActionCard>()
        ).isEmpty()
    }
}
