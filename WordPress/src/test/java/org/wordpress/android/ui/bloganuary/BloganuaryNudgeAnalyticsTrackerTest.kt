package org.wordpress.android.ui.bloganuary

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.bloganuary.BloganuaryNudgeAnalyticsTracker.BloganuaryNudgeCardMenuItem
import org.wordpress.android.ui.bloganuary.learnmore.BloganuaryNudgeLearnMoreOverlayAction
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
    fun `WHEN trackMySiteCardMoreMenuItemTapped is called for hide_this THEN cardsTracker is called correctly`() {
        tracker.trackMySiteCardMoreMenuItemTapped(BloganuaryNudgeCardMenuItem.HIDE_THIS)

        verify(cardsTracker).trackCardMoreMenuItemClicked(
            CardsTracker.Type.BLOGANUARY_NUDGE.label,
            "hide_this"
        )
    }

    @Test
    fun `WHEN trackLearnMoreOverlayShown is called THEN analyticsTracker is called correctly`() {
        listOf(true, false).forEach { isPromptsEnabled ->
            tracker.trackLearnMoreOverlayShown(isPromptsEnabled)

            verify(analyticsTracker).track(
                Stat.BLOGANUARY_NUDGE_LEARN_MORE_MODAL_SHOWN,
                mapOf("prompts_enabled" to isPromptsEnabled.toString())
            )
        }
    }

    @Test
    fun `WHEN trackLearnMoreOverlayDismissed is called THEN analyticsTracker is called correctly`() {
        tracker.trackLearnMoreOverlayDismissed()

        verify(analyticsTracker).track(
            Stat.BLOGANUARY_NUDGE_LEARN_MORE_MODAL_DISMISSED
        )
    }

    @Test
    fun `WHEN trackLearnMoreOverlayActionTapped for dismiss THEN analyticsTracker is called correctly`() {
        tracker.trackLearnMoreOverlayActionTapped(BloganuaryNudgeLearnMoreOverlayAction.DISMISS)

        verify(analyticsTracker).track(
            Stat.BLOGANUARY_NUDGE_LEARN_MORE_MODAL_ACTION_TAPPED,
            mapOf("action" to "dismiss")
        )
    }

    @Test
    fun `WHEN trackLearnMoreOverlayActionTapped for turn_prompts_on THEN analyticsTracker is called correctly`() {
        tracker.trackLearnMoreOverlayActionTapped(BloganuaryNudgeLearnMoreOverlayAction.TURN_ON_PROMPTS)

        verify(analyticsTracker).track(
            Stat.BLOGANUARY_NUDGE_LEARN_MORE_MODAL_ACTION_TAPPED,
            mapOf("action" to "turn_on_prompts")
        )
    }
}
