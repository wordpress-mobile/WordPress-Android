package org.wordpress.android.ui.jetpackoverlay

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseNewUsers
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseSelfHostedUsers
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date

private const val OVERLAY_SHOWN_TIMESTAMP = 1_700_000_000_000L
private const val SHOW_ONCE_ONLY = -1

@RunWith(MockitoJUnitRunner::class)
class JetpackFeatureRemovalOverlayUtilTest {
    @Mock
    private lateinit var phaseHelper: JetpackFeatureRemovalPhaseHelper

    @Mock
    private lateinit var shownTracker: JetpackFeatureOverlayShownTracker

    @Mock
    private lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

    @Mock
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var overlayUtil: JetpackFeatureRemovalOverlayUtil

    @Before
    fun setUp() {
        overlayUtil = JetpackFeatureRemovalOverlayUtil(
            phaseHelper,
            shownTracker,
            dateTimeUtilsWrapper,
            analyticsTrackerWrapper
        )
    }

    @Test
    fun `given no removal phase, when checking the feature collection overlay, then it is not shown`() {
        whenever(phaseHelper.getCurrentPhase()).thenReturn(null)

        assertThat(overlayUtil.shouldShowFeatureCollectionJetpackOverlayForFirstTime()).isFalse
    }

    @Test
    fun `given self hosted phase and overlay never shown, then the overlay is shown`() {
        whenever(phaseHelper.getCurrentPhase()).thenReturn(PhaseSelfHostedUsers)
        whenever(shownTracker.getFeatureCollectionOverlayShown(PhaseSelfHostedUsers)).thenReturn(false)

        assertThat(overlayUtil.shouldShowFeatureCollectionJetpackOverlayForFirstTime()).isTrue
    }

    @Test
    fun `given self hosted phase and overlay already shown, then the overlay is not shown again`() {
        whenever(phaseHelper.getCurrentPhase()).thenReturn(PhaseSelfHostedUsers)
        whenever(shownTracker.getFeatureCollectionOverlayShown(PhaseSelfHostedUsers)).thenReturn(true)

        assertThat(overlayUtil.shouldShowFeatureCollectionJetpackOverlayForFirstTime()).isFalse
    }

    @Test
    fun `given new users phase and overlay never shown, then the overlay is shown`() {
        whenever(phaseHelper.getCurrentPhase()).thenReturn(PhaseNewUsers)
        whenever(shownTracker.getFeatureCollectionOverlayShown(PhaseNewUsers)).thenReturn(false)

        assertThat(overlayUtil.shouldShowFeatureCollectionJetpackOverlayForFirstTime()).isTrue
    }

    // region phase four frequency
    @Test
    fun `given phase four and overlay never shown, then the overlay is shown regardless of frequency`() {
        whenever(phaseHelper.getCurrentPhase()).thenReturn(PhaseFour)
        whenever(shownTracker.getFeatureCollectionOverlayShown(PhaseFour)).thenReturn(false)

        assertThat(overlayUtil.shouldShowFeatureCollectionJetpackOverlayForFirstTime()).isTrue
    }

    @Test
    fun `given phase four already shown and frequency is the show-once sentinel, then it is never shown again`() {
        whenever(phaseHelper.getCurrentPhase()).thenReturn(PhaseFour)
        whenever(shownTracker.getFeatureCollectionOverlayShown(PhaseFour)).thenReturn(true)
        whenever(phaseHelper.getPhaseFourOverlayFrequency()).thenReturn(SHOW_ONCE_ONLY)

        assertThat(overlayUtil.shouldShowFeatureCollectionJetpackOverlayForFirstTime()).isFalse
    }

    @Test
    fun `given phase four already shown and the frequency has elapsed, then the overlay is shown again`() {
        stubPhaseFourShown(frequencyInDays = 7, daysSinceShown = 7)

        assertThat(overlayUtil.shouldShowFeatureCollectionJetpackOverlayForFirstTime()).isTrue
    }

    @Test
    fun `given phase four already shown and the frequency has not elapsed, then the overlay is not shown`() {
        stubPhaseFourShown(frequencyInDays = 7, daysSinceShown = 6)

        assertThat(overlayUtil.shouldShowFeatureCollectionJetpackOverlayForFirstTime()).isFalse
    }

    @Test
    fun `given phase four already shown but no shown timestamp was recorded, then the overlay is not shown`() {
        whenever(phaseHelper.getCurrentPhase()).thenReturn(PhaseFour)
        whenever(shownTracker.getFeatureCollectionOverlayShown(PhaseFour)).thenReturn(true)
        whenever(phaseHelper.getPhaseFourOverlayFrequency()).thenReturn(7)
        whenever(shownTracker.getPhaseFourOverlayShownTimeStamp()).thenReturn(null)

        assertThat(overlayUtil.shouldShowFeatureCollectionJetpackOverlayForFirstTime()).isFalse
    }
    // endregion

    private fun stubPhaseFourShown(frequencyInDays: Int, daysSinceShown: Int) {
        val today = Date(OVERLAY_SHOWN_TIMESTAMP)
        whenever(phaseHelper.getCurrentPhase()).thenReturn(PhaseFour)
        whenever(shownTracker.getFeatureCollectionOverlayShown(PhaseFour)).thenReturn(true)
        whenever(phaseHelper.getPhaseFourOverlayFrequency()).thenReturn(frequencyInDays)
        whenever(shownTracker.getPhaseFourOverlayShownTimeStamp()).thenReturn(OVERLAY_SHOWN_TIMESTAMP)
        whenever(dateTimeUtilsWrapper.getTodaysDate()).thenReturn(today)
        whenever(dateTimeUtilsWrapper.daysBetween(Date(OVERLAY_SHOWN_TIMESTAMP), today))
            .thenReturn(daysSinceShown)
    }
}
