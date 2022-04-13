package org.wordpress.android.ui.mysite.cards.dashboard

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.FooterLink
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard.TodaysStatsCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloggingPromptCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.TodaysStatsCardBuilderParams
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType.DRAFT
import org.wordpress.android.ui.mysite.cards.dashboard.todaysstats.TodaysStatsCardBuilder
import org.wordpress.android.ui.utils.UiString.UiStringText

@RunWith(MockitoJUnitRunner::class)
class CardsBuilderTest : BaseUnitTest() {
    @Mock lateinit var todaysStatsCardBuilder: TodaysStatsCardBuilder
    @Mock lateinit var postCardBuilder: PostCardBuilder
    @Mock lateinit var bloggingPromptCardsBuilder: BloggingPromptCardBuilder
    private lateinit var cardsBuilder: CardsBuilder

    @Before
    fun setUp() {
        cardsBuilder = CardsBuilder(
                todaysStatsCardBuilder,
                postCardBuilder,
                bloggingPromptCardsBuilder
        )
    }

    @Test
    fun `given no stats, when cards are built, then todays stat card is not built`() {
        val cards = buildDashboardCards(hasTodaysStats = false)

        assertThat(cards.findTodaysStatsCard()).isNull()
    }

    @Test
    fun `given stats, when cards are built, then todays stat card is built`() {
        val cards = buildDashboardCards(hasTodaysStats = true)

        assertThat(cards.findTodaysStatsCard()).isNotNull
    }

    /* POST CARD */

    @Test
    fun `given no posts, when cards are built, then post card is not built`() {
        val cards = buildDashboardCards(hasPosts = false)

        assertThat(cards.findPostCard()).isNull()
    }

    @Test
    fun `given posts, when cards are built, then post card is built`() {
        val cards = buildDashboardCards(hasPosts = true)

        assertThat(cards.findPostCard()).isNotNull
    }

    /* BLOGGING PROMPT CARD */

    @Test
    fun `given no blogging prompt, when cards are built, then blogging prompt card is not built`() {
        val cards = buildDashboardCards(hasBlogginPrompt = false)

        assertThat(cards.findBloggingPromptCard()).isNull()
    }

    @Test
    fun `given blogging prompt, when cards are built, then blogging prompt card is built`() {
        val cards = buildDashboardCards(hasBlogginPrompt = true)

        assertThat(cards.findBloggingPromptCard()).isNotNull
    }

    /* ERROR CARD */

    @Test
    fun `given no show error, when cards are built, then error card is not built`() {
        val cards = buildDashboardCards(showErrorCard = false)

        assertThat(cards.findErrorCard()).isNull()
    }

    @Test
    fun `given show error, when cards are built, then error card is built`() {
        val cards = buildDashboardCards(showErrorCard = true)

        assertThat(cards.findErrorCard()).isNotNull
    }

    private fun DashboardCards.findTodaysStatsCard() =
            this.cards.find { it is TodaysStatsCardWithData } as? TodaysStatsCardWithData

    private fun DashboardCards.findPostCard() = this.cards.find { it is PostCard } as? PostCard

    private fun DashboardCards.findBloggingPromptCard() =
            this.cards.find { it is BloggingPromptCard } as? BloggingPromptCard

    private fun DashboardCards.findErrorCard() = this.cards.find { it is ErrorCard } as? ErrorCard

    private val todaysStatsCard = mock<TodaysStatsCardWithData>()

    private val blogingPromptCard = mock<BloggingPromptCardWithData>()

    private fun createPostCards() = listOf(
            PostCardWithPostItems(
                    postCardType = DRAFT,
                    title = UiStringText(""),
                    postItems = emptyList(),
                    footerLink = FooterLink(UiStringText(""), onClick = mock())
            )
    )

    private fun buildDashboardCards(
        hasTodaysStats: Boolean = false,
        hasPosts: Boolean = false,
        hasBlogginPrompt: Boolean = false,
        showErrorCard: Boolean = false
    ): DashboardCards {
        doAnswer { if (hasTodaysStats) todaysStatsCard else null }.whenever(todaysStatsCardBuilder).build(any())
        doAnswer { if (hasPosts) createPostCards() else emptyList() }.whenever(postCardBuilder).build(any())
        doAnswer { if (hasBlogginPrompt) blogingPromptCard else null }.whenever(bloggingPromptCardsBuilder).build(any())
        return cardsBuilder.build(
                dashboardCardsBuilderParams = DashboardCardsBuilderParams(
                        showErrorCard = showErrorCard,
                        onErrorRetryClick = { },
                        todaysStatsCardBuilderParams = TodaysStatsCardBuilderParams(mock(), mock(), mock(), mock()),
                        postCardBuilderParams = PostCardBuilderParams(mock(), mock(), mock()),
                        bloggingPromptCardBuilderParams = BloggingPromptCardBuilderParams(mock(), mock())
                )
        )
    }
}
