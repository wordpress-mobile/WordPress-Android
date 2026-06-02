package org.wordpress.android.ui.mysite.cards.connectivity

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.repositories.EditorSettingsRepository
import org.wordpress.android.ui.accounts.login.CredentialsChangedNotifier
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.NetworkUtilsWrapper

private const val TEST_SITE_LOCAL_ID = 42

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteConnectivityBannerViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var editorSettingsRepository: EditorSettingsRepository

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    lateinit var credentialsChangedNotifier: CredentialsChangedNotifier

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    private val credentialsChangedFlow = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    private lateinit var siteTest: SiteModel
    private lateinit var slice: SiteConnectivityBannerViewModelSlice
    private val emittedBanners = mutableListOf<MySiteCardAndItem?>()

    @Before
    fun setUp() {
        siteTest = SiteModel().apply { id = TEST_SITE_LOCAL_ID }
        // Default network state is available; tests that need offline override per-test. Lenient
        // because tests where the fetch succeeds never reach the network check.
        lenient().`when`(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(credentialsChangedNotifier.events).thenReturn(credentialsChangedFlow)
        slice = SiteConnectivityBannerViewModelSlice(
            editorSettingsRepository,
            networkUtilsWrapper,
            credentialsChangedNotifier,
            selectedSiteRepository,
        )
        slice.initialize(testScope())
        slice.uiModel.observeForever { emittedBanners.add(it) }
    }

    @Test
    fun `given fetch succeeds, when fetchCapabilities invoked, then banner is null`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).thenReturn(true)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()

        assertThat(emittedBanners.last()).isNull()
    }

    @Test
    fun `given fetch fails with no cache, when fetchCapabilities invoked, then banner is shown`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).thenReturn(false)
        whenever(editorSettingsRepository.hasCachedCapabilities(siteTest)).thenReturn(false)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()

        val banner = emittedBanners.last() as MySiteCardAndItem.Item.SingleActionCard
        assertThat(banner.textResource).isEqualTo(R.string.site_connectivity_banner_text)
        assertThat(banner.showLearnMore).isFalse
    }

    @Test
    fun `given fetch fails with no cache but device offline, when fetchCapabilities invoked, then banner is null`() =
        test {
            whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).thenReturn(false)
            whenever(editorSettingsRepository.hasCachedCapabilities(siteTest)).thenReturn(false)
            whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

            slice.fetchCapabilities(siteTest, isUserInitiated = false)
            advanceUntilIdle()

            // Global offline indicator covers this case — suppress to avoid stacked warnings.
            assertThat(emittedBanners.last()).isNull()
        }

    @Test
    fun `given fetch fails but app password pending, when fetchCapabilities invoked, then banner is null`() =
        test {
            whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).thenReturn(false)
            whenever(editorSettingsRepository.hasCachedCapabilities(siteTest)).thenReturn(false)
            whenever(editorSettingsRepository.isAwaitingApplicationPassword(siteTest)).thenReturn(true)

            slice.fetchCapabilities(siteTest, isUserInitiated = false)
            advanceUntilIdle()

            // Credentials are still being minted — pending, not a connection failure.
            assertThat(emittedBanners.last()).isNull()
        }

    @Test
    fun `when credentials change for the selected site, then capabilities are re-fetched`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteTest)
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).thenReturn(true)

        credentialsChangedFlow.emit(TEST_SITE_LOCAL_ID)
        advanceUntilIdle()

        verify(editorSettingsRepository).fetchEditorCapabilitiesForSite(siteTest)
    }

    @Test
    fun `given fetch fails but cache exists, when fetchCapabilities invoked, then banner is null`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).thenReturn(false)
        whenever(editorSettingsRepository.hasCachedCapabilities(siteTest)).thenReturn(true)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()

        assertThat(emittedBanners.last()).isNull()
    }

    @Test
    fun `given prior successful fetch, when fetchCapabilities invoked again non-user-initiated, then fetch skipped`() =
        test {
            whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).thenReturn(true)

            slice.fetchCapabilities(siteTest, isUserInitiated = false)
            advanceUntilIdle()
            slice.fetchCapabilities(siteTest, isUserInitiated = false)
            advanceUntilIdle()

            verify(editorSettingsRepository, times(1)).fetchEditorCapabilitiesForSite(siteTest)
        }

    @Test
    fun `given prior failed fetch, when fetchCapabilities invoked again non-user-initiated, then fetch retries`() =
        test {
            whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).thenReturn(false, true)
            whenever(editorSettingsRepository.hasCachedCapabilities(siteTest)).thenReturn(false)

            slice.fetchCapabilities(siteTest, isUserInitiated = false)
            advanceUntilIdle()
            assertThat(emittedBanners.last()).isNotNull
            slice.fetchCapabilities(siteTest, isUserInitiated = false)
            advanceUntilIdle()

            verify(editorSettingsRepository, times(2)).fetchEditorCapabilitiesForSite(siteTest)
            assertThat(emittedBanners.last()).isNull()
        }

    @Test
    fun `given prior successful fetch, when user-initiated fetchCapabilities invoked, then fetch runs again`() =
        test {
            whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).thenReturn(true)

            slice.fetchCapabilities(siteTest, isUserInitiated = false)
            advanceUntilIdle()
            slice.fetchCapabilities(siteTest, isUserInitiated = true)
            advanceUntilIdle()

            verify(editorSettingsRepository, times(2)).fetchEditorCapabilitiesForSite(siteTest)
        }

    @Test
    fun `given banner showing, when retry tapped, then fetch runs and bypasses session dedup`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).thenReturn(false)
        whenever(editorSettingsRepository.hasCachedCapabilities(siteTest)).thenReturn(false)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()
        val banner = emittedBanners.last() as MySiteCardAndItem.Item.SingleActionCard

        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).thenReturn(true)
        banner.onActionClick()
        advanceUntilIdle()

        verify(editorSettingsRepository, times(2)).fetchEditorCapabilitiesForSite(siteTest)
        assertThat(emittedBanners.last()).isNull()
    }

    @Test
    fun `when clearBanner invoked, then banner is null`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).thenReturn(false)
        whenever(editorSettingsRepository.hasCachedCapabilities(siteTest)).thenReturn(false)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()
        assertThat(emittedBanners.last()).isNotNull
        slice.clearBanner()
        advanceUntilIdle()

        assertThat(emittedBanners.last()).isNull()
    }

    @Test
    fun `given two different sites, when fetched in sequence, then both fetches run`() = test {
        val otherSite = SiteModel().apply { id = TEST_SITE_LOCAL_ID + 1 }
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).thenReturn(true)
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(otherSite)).thenReturn(true)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()
        slice.fetchCapabilities(otherSite, isUserInitiated = false)
        advanceUntilIdle()

        verify(editorSettingsRepository, times(1)).fetchEditorCapabilitiesForSite(eq(siteTest))
        verify(editorSettingsRepository, times(1)).fetchEditorCapabilitiesForSite(eq(otherSite))
    }

    @Test
    fun `given fetch in flight, when clearBanner invoked, then banner stays null after fetch completes`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).thenReturn(false)
        whenever(editorSettingsRepository.hasCachedCapabilities(siteTest)).thenReturn(false)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        slice.clearBanner()
        advanceUntilIdle()

        assertThat(emittedBanners.last()).isNull()
    }

    @Test
    fun `given retry in flight, when banner tapped again, then second tap is a no-op`() = test {
        val gate = CompletableDeferred<Boolean>()
        var callCount = 0
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).doSuspendableAnswer {
            callCount++
            if (callCount == 1) false else gate.await()
        }
        whenever(editorSettingsRepository.hasCachedCapabilities(siteTest)).thenReturn(false)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()
        val banner = emittedBanners.last() as MySiteCardAndItem.Item.SingleActionCard

        banner.onActionClick()    // first tap — retry suspends on gate
        banner.onActionClick()    // second tap — should be ignored
        gate.complete(true)
        advanceUntilIdle()

        verify(editorSettingsRepository, times(2)).fetchEditorCapabilitiesForSite(siteTest)
    }

    @Test
    fun `given banner cleared, when retry tapped, then no fetch runs`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).thenReturn(false)
        whenever(editorSettingsRepository.hasCachedCapabilities(siteTest)).thenReturn(false)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()
        val banner = emittedBanners.last() as MySiteCardAndItem.Item.SingleActionCard
        slice.clearBanner()
        advanceUntilIdle()

        // Simulate a tap that landed before LiveData propagated the null clear.
        banner.onActionClick()
        advanceUntilIdle()

        verify(editorSettingsRepository, times(1)).fetchEditorCapabilitiesForSite(siteTest)
    }

    @Test
    fun `given fetch in flight for site A, when fetch starts for site B, then site A result is discarded`() = test {
        val siteB = SiteModel().apply { id = TEST_SITE_LOCAL_ID + 1 }
        val gateA = CompletableDeferred<Boolean>()
        // Site A's fetch suspends on a gate so we can interleave site B's call before A completes.
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteTest)).doSuspendableAnswer {
            gateA.await()
        }
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(siteB)).thenReturn(true)

        slice.fetchCapabilities(siteTest, isUserInitiated = false)
        advanceUntilIdle()  // siteA suspended in fetch
        slice.fetchCapabilities(siteB, isUserInitiated = false)
        advanceUntilIdle()  // siteB completes; currentSite is now siteB
        gateA.complete(false)  // release siteA — its result must NOT post a banner
        advanceUntilIdle()

        // No banner card should ever have been emitted for site A.
        assertThat(emittedBanners.filterIsInstance<MySiteCardAndItem.Item.SingleActionCard>()).isEmpty()
    }
}
