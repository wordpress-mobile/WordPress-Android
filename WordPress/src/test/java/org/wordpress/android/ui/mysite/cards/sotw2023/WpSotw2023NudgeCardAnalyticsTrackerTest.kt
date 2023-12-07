package org.wordpress.android.ui.mysite.cards.sotw2023

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@OptIn(ExperimentalCoroutinesApi::class)
class WpSotw2023NudgeCardAnalyticsTrackerTest : BaseUnitTest() {
    @Mock
    lateinit var analyticsTracker: AnalyticsTrackerWrapper

    lateinit var tracker: WpSotw2023NudgeCardAnalyticsTracker

    @Before
    fun setUp() {
        tracker = WpSotw2023NudgeCardAnalyticsTracker(analyticsTracker)
    }

    @Test
    fun `WHEN card is shown THEN trackShown is called`() {
        tracker.trackShown()

        verify(analyticsTracker).track(Stat.SOTW_2023_NUDGE_POST_EVENT_CARD_SHOWN)
    }

    @Test
    fun `WHEN card is shown multiple times without resetting THEN trackShown is called once`() {
        tracker.trackShown()
        tracker.trackShown()

        verify(analyticsTracker, times(1)).track(Stat.SOTW_2023_NUDGE_POST_EVENT_CARD_SHOWN)
    }

    @Test
    fun `WHEN card is shown multiple times with resetting THEN trackShown is called multiple times`() {
        tracker.trackShown()
        tracker.resetShown()
        tracker.trackShown()

        verify(analyticsTracker, times(2)).track(Stat.SOTW_2023_NUDGE_POST_EVENT_CARD_SHOWN)
    }

    @Test
    fun `WHEN card CTA is tapped THEN trackCtaTapped is called`() {
        tracker.trackCtaTapped()

        verify(analyticsTracker).track(Stat.SOTW_2023_NUDGE_POST_EVENT_CARD_CTA_TAPPED)
    }

    @Test
    fun `WHEN card hide is tapped THEN trackHideTapped is called`() {
        tracker.trackHideTapped()

        verify(analyticsTracker).track(Stat.SOTW_2023_NUDGE_POST_EVENT_CARD_HIDE_TAPPED)
    }
}
