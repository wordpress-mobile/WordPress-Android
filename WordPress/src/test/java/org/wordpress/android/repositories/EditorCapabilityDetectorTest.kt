package org.wordpress.android.repositories

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.NetworkUtilsWrapper

private const val TEST_SITE_LOCAL_ID = 7

@ExperimentalCoroutinesApi
class EditorCapabilityDetectorTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock
    lateinit var editorSettingsRepository: EditorSettingsRepository

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private lateinit var site: SiteModel
    private lateinit var detector: EditorCapabilityDetector

    @Before
    fun setUp() {
        site = SiteModel().apply { id = TEST_SITE_LOCAL_ID }
        detector = EditorCapabilityDetector(
            editorSettingsRepository,
            networkUtilsWrapper,
            testScope(),
        )
    }

    // region state mapping

    @Test
    fun `given probe succeeds, then state is Ready`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(site)).thenReturn(true)

        val flow = detector.stateFor(site)
        advanceUntilIdle()

        assertThat(flow.value).isEqualTo(EditorCapabilityDetectionState.Ready)
    }

    @Test
    fun `given probe fails but capabilities are cached, then state is Ready`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(site)).thenReturn(false)
        whenever(editorSettingsRepository.hasCachedCapabilities(site)).thenReturn(true)

        val flow = detector.stateFor(site)
        advanceUntilIdle()

        assertThat(flow.value).isEqualTo(EditorCapabilityDetectionState.Ready)
    }

    @Test
    fun `given probe fails while awaiting an application password, then state is Pending`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(site)).thenReturn(false)
        whenever(editorSettingsRepository.hasCachedCapabilities(site)).thenReturn(false)
        whenever(editorSettingsRepository.isAwaitingApplicationPassword(site)).thenReturn(true)

        val flow = detector.stateFor(site)
        advanceUntilIdle()

        assertThat(flow.value).isEqualTo(EditorCapabilityDetectionState.Pending)
    }

    @Test
    fun `given probe fails while offline, then state is TransientError`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(site)).thenReturn(false)
        whenever(editorSettingsRepository.hasCachedCapabilities(site)).thenReturn(false)
        whenever(editorSettingsRepository.isAwaitingApplicationPassword(site)).thenReturn(false)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val flow = detector.stateFor(site)
        advanceUntilIdle()

        assertThat(flow.value).isEqualTo(EditorCapabilityDetectionState.TransientError)
    }

    @Test
    fun `given probe fails online with no pending auth, then state is Unreachable`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(site)).thenReturn(false)
        whenever(editorSettingsRepository.hasCachedCapabilities(site)).thenReturn(false)
        whenever(editorSettingsRepository.isAwaitingApplicationPassword(site)).thenReturn(false)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        val flow = detector.stateFor(site)
        advanceUntilIdle()

        assertThat(flow.value).isEqualTo(EditorCapabilityDetectionState.Unreachable)
    }

    // endregion

    // region deduplication

    @Test
    fun `given a prior successful probe, when stateFor is called again, then it does not re-probe`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(site)).thenReturn(true)

        detector.stateFor(site)
        advanceUntilIdle()
        detector.stateFor(site)
        advanceUntilIdle()

        verify(editorSettingsRepository, times(1)).fetchEditorCapabilitiesForSite(site)
    }

    @Test
    fun `given a prior failed probe, when stateFor is called again, then it re-probes`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(site)).thenReturn(false, true)
        whenever(editorSettingsRepository.hasCachedCapabilities(site)).thenReturn(false)
        whenever(editorSettingsRepository.isAwaitingApplicationPassword(site)).thenReturn(false)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        val flow = detector.stateFor(site)
        advanceUntilIdle()
        assertThat(flow.value).isEqualTo(EditorCapabilityDetectionState.Unreachable)
        detector.stateFor(site)
        advanceUntilIdle()

        verify(editorSettingsRepository, times(2)).fetchEditorCapabilitiesForSite(site)
        assertThat(flow.value).isEqualTo(EditorCapabilityDetectionState.Ready)
    }

    // endregion

    // region refresh

    @Test
    fun `given a prior successful probe, when refresh is called, then it re-probes`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(site)).thenReturn(true)

        detector.stateFor(site)
        advanceUntilIdle()
        detector.refresh(site)
        advanceUntilIdle()

        verify(editorSettingsRepository, times(2)).fetchEditorCapabilitiesForSite(site)
    }

    @Test
    fun `given a probe in flight, when refresh is called, then it does not start a second probe`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(site)).thenReturn(true)

        detector.stateFor(site)  // StandardTestDispatcher: job is launched but not yet run
        detector.refresh(site)   // a probe is already in flight — must be a no-op
        advanceUntilIdle()

        verify(editorSettingsRepository, times(1)).fetchEditorCapabilitiesForSite(site)
    }

    // endregion

    // region awaitProbe

    @Test
    fun `given awaitProbe, then it runs detection and returns the settled state`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(site)).thenReturn(true)

        val state = detector.awaitProbe(site)

        assertThat(state).isEqualTo(EditorCapabilityDetectionState.Ready)
        verify(editorSettingsRepository).fetchEditorCapabilitiesForSite(site)
    }

    @Test
    fun `given a prior successful probe, when awaitProbe is called again, then it does not re-probe`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(site)).thenReturn(true)

        detector.awaitProbe(site)
        detector.awaitProbe(site)

        verify(editorSettingsRepository, times(1)).fetchEditorCapabilitiesForSite(site)
    }

    // endregion

    // region clear

    @Test
    fun `given a probed site, when clear is called, then the next probe runs again`() = test {
        whenever(editorSettingsRepository.fetchEditorCapabilitiesForSite(site)).thenReturn(true)

        detector.awaitProbe(site)
        detector.clear()
        detector.awaitProbe(site)

        verify(editorSettingsRepository, times(2)).fetchEditorCapabilitiesForSite(site)
    }

    @Test
    fun `given no interaction, then no probe runs`() = test {
        verify(editorSettingsRepository, never()).fetchEditorCapabilitiesForSite(site)
    }

    // endregion
}
