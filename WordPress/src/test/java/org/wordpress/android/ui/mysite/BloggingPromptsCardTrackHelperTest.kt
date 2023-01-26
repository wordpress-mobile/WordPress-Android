package org.wordpress.android.ui.mysite

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private lateinit var siteIdFlow: MutableSharedFlow<Int?>
    private lateinit var dashboardCardsFlow: MutableSharedFlow<List<DashboardCards>>

    @Before
    fun setUp() {
        siteIdFlow = MutableSharedFlow()
        dashboardCardsFlow = MutableSharedFlow()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given onResume was called in dashboard, when dashboard cards are received with prompts card, then track once`() =
        test {
            launch {
                val helper = createBloggingPromptsCardTrackHelper()
                helper.onResume(MySiteTabType.DASHBOARD)

                // emit with prompt card (transient state)
                dashboardCardsFlow.emit(
                    listOf(DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>())))
                )

                delay(10)

                // emit again with prompt card (final state) to test debounce
                dashboardCardsFlow.emit(
                    listOf(DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>())))
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
                val helper = createBloggingPromptsCardTrackHelper()
                helper.onResume(MySiteTabType.DASHBOARD)

                // emit with prompt card (transient state)
                dashboardCardsFlow.emit(
                    listOf(DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>())))
                )

                delay(10)

                // emit again without prompt card (final state) to test debounce
                dashboardCardsFlow.emit(
                    listOf(DashboardCards(listOf()))
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
                val helper = createBloggingPromptsCardTrackHelper()

                // emit with prompt card (transient state)
                dashboardCardsFlow.emit(
                    listOf(DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>())))
                )

                delay(10)

                // emit again with prompt card (final state) to test debounce
                dashboardCardsFlow.emit(
                    listOf(DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>())))
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
                val helper = createBloggingPromptsCardTrackHelper()

                // emit with prompt card (transient state)
                dashboardCardsFlow.emit(
                    listOf(DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>())))
                )

                delay(10)

                // emit again without prompt card (final state) to test debounce
                dashboardCardsFlow.emit(
                    listOf(DashboardCards(listOf()))
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
                val helper = createBloggingPromptsCardTrackHelper()
                helper.onResume(MySiteTabType.SITE_MENU)

                // emit with prompt card (final state)
                dashboardCardsFlow.emit(
                    listOf(DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>())))
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
                val helper = createBloggingPromptsCardTrackHelper()

                // emit with prompt card (final state)
                dashboardCardsFlow.emit(
                    listOf(DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>())))
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
            val helper = createBloggingPromptsCardTrackHelper()

            // old site did not have prompt card
            dashboardCardsFlow.emit(
                listOf(DashboardCards(listOf()))
            )

            // simulate the user was here for a while
            delay(1000L)

            // new site selected
            siteIdFlow.emit(1)

            // screen resumed
            helper.onResume(MySiteTabType.DASHBOARD)

            // dashboard cards updated with prompt card
            dashboardCardsFlow.emit(
                listOf(DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>())))
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
            val helper = createBloggingPromptsCardTrackHelper()

            // old site had prompt card
            dashboardCardsFlow.emit(
                listOf(DashboardCards(listOf<DashboardCard>(mock<BloggingPromptCardWithData>())))
            )

            // simulate the user was here for a while
            delay(1000L)

            // new site selected
            siteIdFlow.emit(1)

            // screen resumed
            helper.onResume(MySiteTabType.DASHBOARD)

            // dashboard cards updated without prompt card
            dashboardCardsFlow.emit(
                listOf(DashboardCards(listOf()))
            )

            advanceUntilIdle()

            verify(bloggingPromptsCardAnalyticsTracker, never()).trackMySiteCardViewed()

            // need to cancel this internal job to finish the test
            cancel()
        }
    }

    private fun CoroutineScope.createBloggingPromptsCardTrackHelper() = BloggingPromptsCardTrackHelper(
        this,
        bloggingPromptsCardAnalyticsTracker,
        siteIdFlow,
        dashboardCardsFlow
    )
}
