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
import org.wordpress.android.repositories.EditorCapabilityDetectionState
import org.wordpress.android.repositories.EditorCapabilityDetector
import org.wordpress.android.ui.mysite.MySiteCardAndItem

private const val TEST_SITE_LOCAL_ID = 42

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteConnectivityBannerViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var editorCapabilityDetector: EditorCapabilityDetector

    private lateinit var siteTest: SiteModel
    private lateinit var slice: SiteConnectivityBannerViewModelSlice
    private val emittedBanners = mutableListOf<MySiteCardAndItem?>()

    @Before
    fun setUp() {
        siteTest = SiteModel().apply { id = TEST_SITE_LOCAL_ID }
        slice = SiteConnectivityBannerViewModelSlice(editorCapabilityDetector)
        slice.initialize(testScope())
        slice.uiModel.observeForever { emittedBanners.add(it) }
    }

    private fun stubState(
        site: SiteModel,
        state: EditorCapabilityDetectionState,
    ): MutableStateFlow<EditorCapabilityDetectionState> {
        val flow = MutableStateFlow(state)
        whenever(editorCapabilityDetector.stateFor(site)).thenReturn(flow)
        return flow
    }

    @Test
    fun `given detection unreachable, when fetchCapabilities invoked, then banner is shown`() = test {
        stubState(siteTest, EditorCapabilityDetectionState.Unreachable)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()

        val banner = emittedBanners.last() as MySiteCardAndItem.Item.SingleActionCard
        assertThat(banner.textResource).isEqualTo(R.string.site_connectivity_banner_text)
        assertThat(banner.showLearnMore).isFalse
    }

    @Test
    fun `given detection ready, when fetchCapabilities invoked, then banner is null`() = test {
        stubState(siteTest, EditorCapabilityDetectionState.Ready)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()

        assertThat(emittedBanners.last()).isNull()
    }

    @Test
    fun `given detection pending, when fetchCapabilities invoked, then banner is null`() = test {
        stubState(siteTest, EditorCapabilityDetectionState.Pending)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()

        // Pending = probing or awaiting credentials — never a false "can't connect".
        assertThat(emittedBanners.last()).isNull()
    }

    @Test
    fun `given detection transient error, when fetchCapabilities invoked, then banner is null`() = test {
        stubState(siteTest, EditorCapabilityDetectionState.TransientError)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()

        // Transient (e.g. offline) is covered by the global indicator — don't stack a warning.
        assertThat(emittedBanners.last()).isNull()
    }

    @Test
    fun `given unreachable then recovered to ready, when state changes, then banner clears`() = test {
        val flow = stubState(siteTest, EditorCapabilityDetectionState.Unreachable)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()
        assertThat(emittedBanners.last()).isNotNull

        flow.value = EditorCapabilityDetectionState.Ready
        advanceUntilIdle()

        assertThat(emittedBanners.last()).isNull()
    }

    @Test
    fun `given user-initiated, when fetchCapabilities invoked, then detector is refreshed`() = test {
        stubState(siteTest, EditorCapabilityDetectionState.Ready)

        slice.fetchCapabilities(siteTest, isUserInitiated = true)
        advanceUntilIdle()

        verify(editorCapabilityDetector).refresh(siteTest)
    }

    @Test
    fun `given non-user-initiated, when fetchCapabilities invoked, then detector is not refreshed`() = test {
        stubState(siteTest, EditorCapabilityDetectionState.Ready)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()

        verify(editorCapabilityDetector, never()).refresh(any())
    }

    @Test
    fun `given banner showing, when retry tapped, then detector is refreshed`() = test {
        stubState(siteTest, EditorCapabilityDetectionState.Unreachable)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()
        val banner = emittedBanners.last() as MySiteCardAndItem.Item.SingleActionCard

        banner.onActionClick()
        advanceUntilIdle()

        verify(editorCapabilityDetector).refresh(siteTest)
    }

    @Test
    fun `when clearBanner invoked, then banner is null`() = test {
        stubState(siteTest, EditorCapabilityDetectionState.Unreachable)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()
        assertThat(emittedBanners.last()).isNotNull
        slice.clearBanner()
        advanceUntilIdle()

        assertThat(emittedBanners.last()).isNull()
    }

    @Test
    fun `given banner cleared, when retry tapped, then no refresh runs`() = test {
        stubState(siteTest, EditorCapabilityDetectionState.Unreachable)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()
        val banner = emittedBanners.last() as MySiteCardAndItem.Item.SingleActionCard
        slice.clearBanner()
        advanceUntilIdle()

        // Simulate a tap that landed before LiveData propagated the null clear.
        banner.onActionClick()
        advanceUntilIdle()

        verify(editorCapabilityDetector, never()).refresh(any())
    }

    @Test
    fun `given site switched, when old site becomes unreachable, then banner ignores it`() = test {
        val siteB = SiteModel().apply { id = TEST_SITE_LOCAL_ID + 1 }
        val flowA = stubState(siteTest, EditorCapabilityDetectionState.Ready)
        stubState(siteB, EditorCapabilityDetectionState.Ready)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()
        slice.fetchCapabilities(siteB, isUserInitiated = false)
        advanceUntilIdle()

        // Site A's probe resolves to Unreachable after we've switched to B — must not surface.
        flowA.value = EditorCapabilityDetectionState.Unreachable
        advanceUntilIdle()

        assertThat(
            emittedBanners.filterIsInstance<MySiteCardAndItem.Item.SingleActionCard>()
        ).isEmpty()
    }
}
