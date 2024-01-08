package org.wordpress.android.ui.mysite.cards.dynamiccard

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DYNAMIC_DASHBOARD_CARD_CTA_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DYNAMIC_DASHBOARD_CARD_HIDE_TAPPED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DYNAMIC_DASHBOARD_CARD_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DYNAMIC_DASHBOARD_CARD_TAPPED
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner::class)
class DynamicCardsAnalyticsTrackerTest {
    @Mock
    private lateinit var wrapper: AnalyticsTrackerWrapper

    @InjectMocks
    private lateinit var tracker: DynamicCardsAnalyticsTracker

    @Test
    fun `WHEN card is shown THEN track DYNAMIC_DASHBOARD_CARD_SHOWN event`() {
        tracker.trackShown("id")

        verify(wrapper).track(DYNAMIC_DASHBOARD_CARD_SHOWN, mapOf("id" to "id"))
    }

    @Test
    fun `GIVEN shown event tracked WHEN card is shown THEN do not track DYNAMIC_DASHBOARD_CARD_SHOWN event again`() {
        tracker.trackShown("id")
        tracker.trackShown("id")

        verify(wrapper, times(1)).track(DYNAMIC_DASHBOARD_CARD_SHOWN, mapOf("id" to "id"))
    }

    @Test
    @Suppress("MaxLineLength")
    fun `GIVEN shown first event tracked WHEN another card is shown THEN track DYNAMIC_DASHBOARD_CARD_SHOWN event again for the new card`() {
        tracker.trackShown("id")
        tracker.trackShown("id_two")

        verify(wrapper).track(DYNAMIC_DASHBOARD_CARD_SHOWN, mapOf("id" to "id"))
        verify(wrapper).track(DYNAMIC_DASHBOARD_CARD_SHOWN, mapOf("id" to "id_two"))
    }

    @Test
    fun `WHEN card is clicked THEN track DYNAMIC_DASHBOARD_CARD_TAPPED event`() {
        tracker.trackCardTapped("id", "url")

        verify(wrapper).track(DYNAMIC_DASHBOARD_CARD_TAPPED, mapOf("id" to "id", "url" to "url"))
    }

    @Test
    fun `WHEN card cta is clicked THEN track DYNAMIC_DASHBOARD_CARD_CTA_TAPPED event`() {
        tracker.trackCtaTapped("id", "url")

        verify(wrapper).track(DYNAMIC_DASHBOARD_CARD_CTA_TAPPED, mapOf("id" to "id", "url" to "url"))
    }

    @Test
    fun `WHEN card is hidden THEN track DYNAMIC_DASHBOARD_CARD_HIDE_TAPPED event`() {
        tracker.trackHideTapped("id")

        verify(wrapper).track(DYNAMIC_DASHBOARD_CARD_HIDE_TAPPED, mapOf("id" to "id"))
    }
}
