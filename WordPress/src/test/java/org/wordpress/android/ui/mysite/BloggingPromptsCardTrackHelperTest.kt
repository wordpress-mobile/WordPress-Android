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
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptsCardAnalyticsTracker
import org.wordpress.android.ui.mysite.tabs.MySiteTabType

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
                helper.onResume(MySiteTabType.DASHBOARD)

                // with prompt card (transient state)
                helper.onDashboardCardsUpdated(
                    this,
                    DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>()))
                )

                delay(10)

                // again with prompt card (final state) to test debounce
                helper.onDashboardCardsUpdated(
                    this,
                    DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>()))
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
                helper.onResume(MySiteTabType.DASHBOARD)

                // with prompt card (transient state)
                helper.onDashboardCardsUpdated(
                    this,
                    DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>()))
                )

                delay(10)

                // again without prompt card (final state) to test debounce
                helper.onDashboardCardsUpdated(
                    this,
                    DashboardCards(listOf())
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
                // with prompt card (transient state)
                helper.onDashboardCardsUpdated(
                    this,
                    DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>()))
                )

                delay(10)

                // again with prompt card (final state) to test debounce
                helper.onDashboardCardsUpdated(
                    this,
                    DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>()))
                )

                advanceUntilIdle()

                helper.onResume(MySiteTabType.DASHBOARD)

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
                // with prompt card (transient state)
                helper.onDashboardCardsUpdated(
                    this,
                    DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>()))
                )

                delay(10)

                // again without prompt card (final state) to test debounce
                helper.onDashboardCardsUpdated(
                    this,
                    DashboardCards(listOf())
                )

                advanceUntilIdle()

                helper.onResume(MySiteTabType.DASHBOARD)

                verify(bloggingPromptsCardAnalyticsTracker, never()).trackMySiteCardViewed()

                // need to cancel this internal job to finish the test
                cancel()
            }
        }

    @Test
    fun `given onResume was called in menu, when dashboard cards are received with prompts card, then don't track`() =
        test {
            launch {
                helper.onResume(MySiteTabType.SITE_MENU)

                // with prompt card (final state)
                helper.onDashboardCardsUpdated(
                    this,
                    DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>()))
                )

                advanceUntilIdle()

                verify(bloggingPromptsCardAnalyticsTracker, never()).trackMySiteCardViewed()

                // need to cancel this internal job to finish the test
                cancel()
            }
        }

    @Test
    fun `given dashboard cards were received with prompts card, when onResume is called in menu, then don't track`() =
        test {
            launch {
                // with prompt card (final state)
                helper.onDashboardCardsUpdated(
                    this,
                    DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>()))
                )

                advanceUntilIdle()

                helper.onResume(MySiteTabType.SITE_MENU)

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
                DashboardCards(listOf())
            )

            // simulate the user was here for a while
            delay(1000L)

            // new site selected
            helper.onSiteChanged(1)

            // screen resumed
            helper.onResume(MySiteTabType.DASHBOARD)

            // dashboard cards updated with prompt card
            helper.onDashboardCardsUpdated(
                this,
                DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>()))
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
            helper.onDashboardCardsUpdated(
                this,
                DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>()))
            )

            // simulate the user was here for a while
            delay(1000L)

            // new site selected
            helper.onSiteChanged(1)

            // screen resumed
            helper.onResume(MySiteTabType.DASHBOARD)

            // dashboard cards updated without prompt card
            helper.onDashboardCardsUpdated(
                this,
                DashboardCards(listOf())
            )

            advanceUntilIdle()

            verify(bloggingPromptsCardAnalyticsTracker, never()).trackMySiteCardViewed()

            // need to cancel this internal job to finish the test
            cancel()
        }
    }
}
