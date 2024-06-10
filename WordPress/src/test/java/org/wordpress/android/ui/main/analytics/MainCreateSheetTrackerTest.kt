package org.wordpress.android.ui.main.analytics

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.main.MainActionListItem
import org.wordpress.android.ui.main.WPMainNavigationView
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptAttribution
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner::class)
class MainCreateSheetTrackerTest {
    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private lateinit var tracker: MainCreateSheetTracker

    @Before
    fun setUp() {
        tracker = MainCreateSheetTracker(analyticsTracker)
    }

    // region trackActionTapped
    @Test
    fun `trackActionTapped tracks action tapped for my site page`() {
        // Arrange
        val page = WPMainNavigationView.PageType.MY_SITE
        val actionType = MainActionListItem.ActionType.CREATE_NEW_POST
        val expectedStat = AnalyticsTracker.Stat.MY_SITE_CREATE_SHEET_ACTION_TAPPED

        // Act
        tracker.trackActionTapped(page, actionType)

        // Assert
        verify(analyticsTracker).track(eq(expectedStat), argThat<Map<String, Any>> {
            this["action"] == "create_new_post"
        })
    }

    @Test
    fun `trackActionTapped tracks action tapped for reader page`() {
        // Arrange
        val page = WPMainNavigationView.PageType.READER
        val actionType = MainActionListItem.ActionType.CREATE_NEW_POST
        val expectedStat = AnalyticsTracker.Stat.READER_CREATE_SHEET_ACTION_TAPPED

        // Act
        tracker.trackActionTapped(page, actionType)

        // Assert
        verify(analyticsTracker).track(eq(expectedStat), argThat<Map<String, Any>> {
            this["action"] == "create_new_post"
        })
    }

    @Test
    fun `trackActionTapped does not track action tapped for other pages`() {
        WPMainNavigationView.PageType.entries
            .filterNot { it == WPMainNavigationView.PageType.MY_SITE || it == WPMainNavigationView.PageType.READER }
            .forEach { page ->
                // Arrange
                val actionType = MainActionListItem.ActionType.CREATE_NEW_POST

                // Act
                tracker.trackActionTapped(page, actionType)

                // Assert
                verifyNoInteractions(analyticsTracker)
            }
    }
    // endregion

    // region trackAnswerPromptActionTapped
    @Test
    fun `trackAnswerPromptActionTapped tracks answer prompt action tapped for my site page`() {
        // Arrange
        val page = WPMainNavigationView.PageType.MY_SITE
        val attribution = BloggingPromptAttribution.DAY_ONE
        val expectedStat = AnalyticsTracker.Stat.MY_SITE_CREATE_SHEET_ANSWER_PROMPT_TAPPED

        // Act
        tracker.trackAnswerPromptActionTapped(page, attribution)

        // Assert
        verify(analyticsTracker).track(eq(expectedStat), argThat<Map<String, Any>> {
            this["attribution"] == attribution.value
        })
    }

    @Test
    fun `trackAnswerPromptActionTapped tracks answer prompt action tapped for reader page`() {
        // Arrange
        val page = WPMainNavigationView.PageType.READER
        val attribution = BloggingPromptAttribution.DAY_ONE
        val expectedStat = AnalyticsTracker.Stat.READER_CREATE_SHEET_ANSWER_PROMPT_TAPPED

        // Act
        tracker.trackAnswerPromptActionTapped(page, attribution)

        // Assert
        verify(analyticsTracker).track(eq(expectedStat), argThat<Map<String, Any>> {
            this["attribution"] == attribution.value
        })
    }

    @Test
    fun `trackAnswerPromptActionTapped does not track answer prompt action tapped for other pages`() {
        WPMainNavigationView.PageType.entries
            .filterNot { it == WPMainNavigationView.PageType.MY_SITE || it == WPMainNavigationView.PageType.READER }
            .forEach { page ->
                // Arrange
                val attribution = BloggingPromptAttribution.DAY_ONE

                // Act
                tracker.trackAnswerPromptActionTapped(page, attribution)

                // Assert
                verifyNoInteractions(analyticsTracker)
            }
    }
    // endregion

    // region trackHelpPromptActionTapped
    @Test
    fun `trackHelpPromptActionTapped tracks help prompt action tapped for my site page`() {
        // Arrange
        val page = WPMainNavigationView.PageType.MY_SITE
        val expectedStat = AnalyticsTracker.Stat.MY_SITE_CREATE_SHEET_PROMPT_HELP_TAPPED

        // Act
        tracker.trackHelpPromptActionTapped(page)

        // Assert
        verify(analyticsTracker).track(expectedStat)
    }

    @Test
    fun `trackHelpPromptActionTapped tracks help prompt action tapped for reader page`() {
        // Arrange
        val page = WPMainNavigationView.PageType.READER
        val expectedStat = AnalyticsTracker.Stat.READER_CREATE_SHEET_PROMPT_HELP_TAPPED

        // Act
        tracker.trackHelpPromptActionTapped(page)

        // Assert
        verify(analyticsTracker).track(expectedStat)
    }

    @Test
    fun `trackHelpPromptActionTapped does not track help prompt action tapped for other pages`() {
        WPMainNavigationView.PageType.entries
            .filterNot { it == WPMainNavigationView.PageType.MY_SITE || it == WPMainNavigationView.PageType.READER }
            .forEach { page ->
                // Act
                tracker.trackHelpPromptActionTapped(page)

                // Assert
                verifyNoInteractions(analyticsTracker)
            }
    }
    // endregion

    // region trackSheetShown
    @Test
    fun `trackSheetShown tracks sheet shown for my site page`() {
        // Arrange
        val page = WPMainNavigationView.PageType.MY_SITE
        val expectedStat = AnalyticsTracker.Stat.MY_SITE_CREATE_SHEET_SHOWN

        // Act
        tracker.trackSheetShown(page)

        // Assert
        verify(analyticsTracker).track(expectedStat)
    }

    @Test
    fun `trackSheetShown tracks sheet shown for reader page`() {
        // Arrange
        val page = WPMainNavigationView.PageType.READER
        val expectedStat = AnalyticsTracker.Stat.READER_CREATE_SHEET_SHOWN

        // Act
        tracker.trackSheetShown(page)

        // Assert
        verify(analyticsTracker).track(expectedStat)
    }

    @Test
    fun `trackSheetShown does not track sheet shown for other pages`() {
        WPMainNavigationView.PageType.entries
            .filterNot { it == WPMainNavigationView.PageType.MY_SITE || it == WPMainNavigationView.PageType.READER }
            .forEach { page ->
                // Act
                tracker.trackSheetShown(page)

                // Assert
                verifyNoInteractions(analyticsTracker)
            }
    }
    // endregion

    // region trackFabShown
    @Test
    fun `trackFabShown tracks fab shown for my site page`() {
        // Arrange
        val page = WPMainNavigationView.PageType.MY_SITE
        val expectedStat = AnalyticsTracker.Stat.MY_SITE_CREATE_FAB_SHOWN

        // Act
        tracker.trackFabShown(page)

        // Assert
        verify(analyticsTracker).track(expectedStat)
    }

    @Test
    fun `trackFabShown tracks fab shown for reader page`() {
        // Arrange
        val page = WPMainNavigationView.PageType.READER
        val expectedStat = AnalyticsTracker.Stat.READER_CREATE_FAB_SHOWN

        // Act
        tracker.trackFabShown(page)

        // Assert
        verify(analyticsTracker).track(expectedStat)
    }

    @Test
    fun `trackFabShown does not track fab shown for other pages`() {
        WPMainNavigationView.PageType.entries
            .filterNot { it == WPMainNavigationView.PageType.MY_SITE || it == WPMainNavigationView.PageType.READER }
            .forEach { page ->
                // Act
                tracker.trackFabShown(page)

                // Assert
                verifyNoInteractions(analyticsTracker)
            }
    }
    // endregion

    // region trackCreateActionsSheetCard
    @Test
    fun `trackCreateActionsSheetCard tracks bottom sheet when it is in the list`() {
        val actionList = listOf(
            mock<MainActionListItem.AnswerBloggingPromptAction>(),
            mock<MainActionListItem.CreateAction>(),
            mock<MainActionListItem.CreateAction>(),
        )
        tracker.trackCreateActionsSheetCard(actionList)
        verify(analyticsTracker).track(AnalyticsTracker.Stat.BLOGGING_PROMPTS_CREATE_SHEET_CARD_VIEWED)
    }

    @Test
    fun `trackCreateActionsSheetCard does not track bottom sheet when it is not in the list`() {
        val actionList = listOf(
            mock<MainActionListItem.CreateAction>(),
            mock<MainActionListItem.CreateAction>(),
        )
        tracker.trackCreateActionsSheetCard(actionList)
        verifyNoInteractions(analyticsTracker)
    }
    // endregion
}
