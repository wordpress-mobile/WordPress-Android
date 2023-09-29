package org.wordpress.android.ui.mysite

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptsCardAnalyticsTracker

@ExperimentalCoroutinesApi
class BloggingPromptsCardTrackHelperTest : BaseUnitTest() {
    private val bloggingPromptsCardAnalyticsTracker: BloggingPromptsCardAnalyticsTracker = mock()

    private lateinit var helper: BloggingPromptsCardTrackHelper

    @Before
    fun setUp() {
        helper = BloggingPromptsCardTrackHelper(bloggingPromptsCardAnalyticsTracker, testDispatcher())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given onResume was called in dashboard, when dashboard cards are received with prompts card, then track once`() =
        test {
            launch {
                helper.onResume()

                // with prompt card (transient state)
                helper.onDashboardCardsUpdated(
                    this,
                    mock()
                )

                delay(10)

                // again with prompt card (final state) to test debounce
                helper.onDashboardCardsUpdated(
                    this,
                    mock()
                )

                advanceUntilIdle()

                verify(bloggingPromptsCardAnalyticsTracker).trackMySiteCardViewed()

                // need to cancel this internal job to finish the test
                cancel()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `given onResume was called in dashboard, when dashboard cards are received without prompts card, then don't track`() =
        test {
            launch {
                helper.onResume()

                val bloggingPromptCards = mock<List<MySiteCardAndItem.Card.BloggingPromptCard>>()
                // with prompt card (transient state)
                helper.onDashboardCardsUpdated(
                    this,
                    bloggingPromptCards
                )

                delay(10)

                // again without prompt card (final state) to test debounce
                helper.onDashboardCardsUpdated(
                    this,
                    listOf()
                )

                advanceUntilIdle()

                verify(bloggingPromptsCardAnalyticsTracker, never()).trackMySiteCardViewed()

                // need to cancel this internal job to finish the test
                cancel()
            }
        }

    @Suppress("MaxLineLength")

    @Test
    fun `given dashboard cards were received with prompts card, when onResume is called in dashboard, then track once`() =
        test {
            launch {
                val bloggingPromptCards = mock<List<MySiteCardAndItem.Card.BloggingPromptCard>>()
                // with prompt card (transient state)
                helper.onDashboardCardsUpdated(
                    this,
                    bloggingPromptCards
                )

                delay(10)

                // again with prompt card (final state) to test debounce
                helper.onDashboardCardsUpdated(
                    this,
                    bloggingPromptCards
                )

                advanceUntilIdle()

                helper.onResume()

                verify(bloggingPromptsCardAnalyticsTracker).trackMySiteCardViewed()

                // need to cancel this internal job to finish the test
                cancel()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `given dashboard cards were received without prompts card, when onResume is called in dashboard, then don't track`() =
        test {
            launch {
                val bloggingPromptCards = mock<List<MySiteCardAndItem.Card.BloggingPromptCard>>()
                // with prompt card (transient state)
                helper.onDashboardCardsUpdated(
                    this,
                    bloggingPromptCards
                )

                delay(10)

                // again without prompt card (final state) to test debounce
                helper.onDashboardCardsUpdated(
                    this,
                    listOf()
                )

                advanceUntilIdle()

                helper.onResume()

                verify(bloggingPromptsCardAnalyticsTracker, never()).trackMySiteCardViewed()

                // need to cancel this internal job to finish the test
                cancel()
            }
        }

    @Test
    fun `given new site selected, when dashboard cards are updated with prompt card, then track once`() = test {
        launch {
            // old site did not have prompt card
            helper.onDashboardCardsUpdated(
                this,
                mock()
            )

            // simulate the user was here for a while
            delay(1000L)

            // new site selected
            helper.onSiteChanged(1)

            // screen resumed
            helper.onResume()

            // dashboard cards updated with prompt card
            helper.onDashboardCardsUpdated(
                this,
                mock()
            )

            advanceUntilIdle()

            verify(bloggingPromptsCardAnalyticsTracker).trackMySiteCardViewed()

            // need to cancel this internal job to finish the test
            cancel()
        }
    }

    @Test
    fun `given new site selected, when dashboard cards are updated without prompt card, then don't track`() = test {
        launch {
            // old site had prompt card
            val bloggingPromptCards = mock<List<MySiteCardAndItem.Card.BloggingPromptCard>>()

            helper.onDashboardCardsUpdated(
                this,
                bloggingPromptCards
            )

            // simulate the user was here for a while
            delay(1000L)

            // new site selected
            helper.onSiteChanged(1)

            // screen resumed
            helper.onResume()

            // dashboard cards updated without prompt card
            helper.onDashboardCardsUpdated(
                this,
                listOf()
            )

            advanceUntilIdle()

            verify(bloggingPromptsCardAnalyticsTracker, never()).trackMySiteCardViewed()

            // need to cancel this internal job to finish the test
            cancel()
        }
    }
}
