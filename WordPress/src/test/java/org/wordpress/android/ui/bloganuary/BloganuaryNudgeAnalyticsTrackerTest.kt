package org.wordpress.android.ui.bloganuary

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.bloganuary.BloganuaryNudgeAnalyticsTracker.BloganuaryNudgeCardMenuItem
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@OptIn(ExperimentalCoroutinesApi::class)
class BloganuaryNudgeAnalyticsTrackerTest : BaseUnitTest() {
    @Mock
    lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Mock
    lateinit var cardsTracker: CardsTracker

    lateinit var tracker: BloganuaryNudgeAnalyticsTracker

    @Before
    fun setUp() {
        tracker = BloganuaryNudgeAnalyticsTracker(analyticsTracker, cardsTracker)
    }

    @Test
    fun `WHEN trackMySiteCardLearnMoreTapped is called THEN analyticsTracker is called correctly`() {
        listOf(true, false).forEach { isPromptsEnabled ->
            tracker.trackMySiteCardLearnMoreTapped(isPromptsEnabled)

            verify(analyticsTracker).track(
                Stat.BLOGANUARY_NUDGE_MY_SITE_CARD_LEARN_MORE_TAPPED,
                mapOf("prompts_enabled" to isPromptsEnabled.toString())
            )
        }
    }

    @Test
    fun `WHEN trackMySiteCardMoreMenuTapped is called THEN cardsTracker is called correctly`() {
        tracker.trackMySiteCardMoreMenuTapped()

        verify(cardsTracker).trackCardMoreMenuClicked(
            CardsTracker.Type.BLOGANUARY_NUDGE.label
        )
    }

    @Test
    fun `WHEN trackMySiteCardMoreMenuItemTapped is called THEN cardsTracker is called correctly`() {
        BloganuaryNudgeCardMenuItem.entries.forEach {
            tracker.trackMySiteCardMoreMenuItemTapped(it)

            verify(cardsTracker).trackCardMoreMenuItemClicked(
                CardsTracker.Type.BLOGANUARY_NUDGE.label,
                it.label
            )
        }
    }
}
