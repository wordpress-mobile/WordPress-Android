package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class BloggingPromptsCardAnalyticsTrackerTest {
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val classToTest = BloggingPromptsCardAnalyticsTracker(analyticsTracker)

    @Test
    fun `Should track my site card answer prompt clicked`() {
        classToTest.trackMySiteCardAnswerPromptClicked()
        verify(analyticsTracker).track(
                Stat.BLOGGING_PROMPTS_MY_SITE_CARD_ANSWER_PROMPT_CLICKED,
                emptyMap()
        )
    }

    @Test
    fun `Should track my site card share button clicked`() {
        classToTest.trackMySiteCardShareClicked()
        verify(analyticsTracker).track(
                Stat.BLOGGING_PROMPTS_MY_SITE_CARD_SHARE_CLICKED,
                emptyMap()
        )
    }

    @Test
    fun `Should track my site card menu clicked`() {
        classToTest.trackMySiteCardMenuClicked()
        verify(analyticsTracker).track(
                Stat.BLOGGING_PROMPTS_MY_SITE_CARD_MENU_CLICKED,
                emptyMap()
        )
    }

    @Test
    fun `Should track my site card menu view more prompts clicked`() {
        classToTest.trackMySiteCardMenuViewMorePromptsClicked()
        verify(analyticsTracker).track(
                Stat.BLOGGING_PROMPTS_MY_SITE_CARD_MENU_VIEW_MORE_PROMPTS_CLICKED,
                emptyMap()
        )
    }

    @Test
    fun `Should track my site card menu skip this prompt clicked`() {
        classToTest.trackMySiteCardMenuSkipThisPromptClicked()
        verify(analyticsTracker).track(
                Stat.BLOGGING_PROMPTS_MY_SITE_CARD_MENU_SKIP_THIS_PROMPT_CLICKED,
                emptyMap()
        )
    }

    @Test
    fun `Should track my site card menu remove from dashboard clicked`() {
        classToTest.trackMySiteCardMenuRemoveFromDashboardClicked()
        verify(analyticsTracker).track(
                Stat.BLOGGING_PROMPTS_MY_SITE_CARD_MENU_REMOVE_FROM_DASHBOARD_CLICKED,
                emptyMap()
        )
    }

    @Test
    fun `Should track my site card menu learn more clicked`() {
        classToTest.trackMySiteCardMenuLearnMoreClicked()
        verify(analyticsTracker).track(
                Stat.BLOGGING_PROMPTS_MY_SITE_CARD_MENU_LEARN_MORE_CLICKED,
                emptyMap()
        )
    }

    @Test
    fun `Should track my site card skip this prompt undo clicked`() {
        classToTest.trackMySiteCardSkipThisPromptUndoClicked()
        verify(analyticsTracker).track(
                Stat.BLOGGING_PROMPTS_MY_SITE_CARD_MENU_SKIP_THIS_PROMPT_UNDO_CLICKED,
                emptyMap()
        )
    }

    @Test
    fun `Should track my site card remove from dashboard undo clicked`() {
        classToTest.trackMySiteCardRemoveFromDashboardUndoClicked()
        verify(analyticsTracker).track(
                Stat.BLOGGING_PROMPTS_MY_SITE_CARD_MENU_REMOVE_FROM_DASHBOARD_UNDO_CLICKED,
                emptyMap()
        )
    }

    @Test
    fun `Should track my site card viewed`() {
        classToTest.trackMySiteCardViewed()
        verify(analyticsTracker).track(Stat.BLOGGING_PROMPTS_MY_SITE_CARD_VIEWED, emptyMap())
    }
}
