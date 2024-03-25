package org.wordpress.android.ui.mysite

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptAttribution
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptsCardAnalyticsTracker
import org.wordpress.android.ui.utils.UiString

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
                // with prompt card (transient state)
                helper.onDashboardCardsUpdated(
                    this, getBloggingPromptCards()
                )

                delay(10)

                // again with prompt card (final state) to test debounce
                helper.onDashboardCardsUpdated(
                    this, getBloggingPromptCards()
                )

                advanceUntilIdle()

                verify(bloggingPromptsCardAnalyticsTracker).trackMySiteCardViewed("bloganuary")

                // need to cancel this internal job to finish the test
                cancel()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `given onResume was called in dashboard, when dashboard cards are received without prompts card, then don't track`() =
        test {
            launch {
                // without prompt card (transient state)
                helper.onDashboardCardsUpdated(
                    this,
                    emptyList()
                )

                delay(500L)

                // again with prompt card (final state) to test debounce
                helper.onDashboardCardsUpdated(
                    this,
                    getBloggingPromptCards()
                )

                advanceUntilIdle()

                verify(bloggingPromptsCardAnalyticsTracker).trackMySiteCardViewed("bloganuary")

                // need to cancel this internal job to finish the test
                cancel()
            }
        }


    @Test
    fun `given new site selected, when dashboard cards are updated with prompt card, then track`() = test {
        launch {
            // old site have prompt card
            helper.onDashboardCardsUpdated(
                this,
                getBloggingPromptCards()
            )

            // simulate the user was here for a while
            delay(1000L)

            // new site selected
            helper.onSiteChanged()

            // dashboard cards updated with prompt card
            helper.onDashboardCardsUpdated(
                this, getBloggingPromptCards()
            )

            advanceUntilIdle()

            verify(bloggingPromptsCardAnalyticsTracker, atLeast(2)).trackMySiteCardViewed("bloganuary")

            // need to cancel this internal job to finish the test
            cancel()
        }
    }

    @Test
    fun `given new site selected, when dashboard cards are updated without prompt card, then don't track`() = test {
        launch {
            helper.onDashboardCardsUpdated(
                this,
                getBloggingPromptCards()
            )

            // simulate the user was here for a while
            delay(1000L)

            // new site selected
            helper.onSiteChanged()

            // dashboard cards updated without prompt card
            helper.onDashboardCardsUpdated(
                this,
                emptyList()
            )

            advanceUntilIdle()

            verify(bloggingPromptsCardAnalyticsTracker, never()).trackMySiteCardViewed("attribution")

            // need to cancel this internal job to finish the test
            cancel()
        }
    }

    private fun getBloggingPromptCards() = listOf(
            MySiteCardAndItem.Card.BloggingPromptCard.BloggingPromptCardWithData(
                prompt = UiString.UiStringText("prompt"),
                respondents = listOf(
                    TrainOfAvatarsItem.AvatarItem("url")
                ),
                numberOfAnswers = 5,
                isAnswered = true,
                promptId = 123,
                tagUrl = "tagUrl",
                attribution = BloggingPromptAttribution.BLOGANUARY,
                onShareClick = {},
                onAnswerClick = {},
                onSkipClick = {},
                onViewMoreClick = {},
                onViewAnswersClick = {},
                onRemoveClick = {},
            )
        )
}
