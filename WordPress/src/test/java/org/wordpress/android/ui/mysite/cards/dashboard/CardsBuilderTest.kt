package org.wordpress.android.ui.mysite.cards.dashboard

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.FooterLink
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithoutPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard.TodaysStatsCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloggingPromptCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.TodaysStatsCardBuilderParams
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType.CREATE_FIRST
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType.DRAFT
import org.wordpress.android.ui.mysite.cards.dashboard.todaysstats.TodaysStatsCardBuilder
import org.wordpress.android.ui.utils.UiString.UiStringText

@ExperimentalCoroutinesApi
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
        val cards = buildDashboardCards(hasPostsForPostCard = false)

        assertThat(cards.findPostCardWithPosts()).isNull()
    }

    @Test
    fun `given posts, when cards are built, then post card is built`() {
        val cards = buildDashboardCards(hasPostsForPostCard = true)

        assertThat(cards.findPostCardWithPosts()).isNotNull
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

    /* BLOGGING PROMPT AND POST CARD */

    @Test
    fun `given blogging prompt and posts, both prompt and post cards are visible`() {
        val cards = buildDashboardCards(hasBlogginPrompt = true, hasPostsForPostCard = true)

        assertThat(cards.findBloggingPromptCard()).isNotNull
        assertThat(cards.findPostCardWithPosts()).isNotNull
    }

    @Test
    fun `given blogging prompt and no posts, prompt card is visible while post and next post cards are not`() {
        val cards = buildDashboardCards(hasBlogginPrompt = true, hasPostsForPostCard = false)

        assertThat(cards.findBloggingPromptCard()).isNotNull
        assertThat(cards.findPostCardWithPosts()).isNull()
        assertThat(cards.findNextPostCard()).isNull()
    }

    @Test
    fun `given no blogging prompt and no posts, next post card is visible and prompt card is not`() {
        val cards = buildDashboardCards(hasBlogginPrompt = false, hasPostsForPostCard = false)

        assertThat(cards.findBloggingPromptCard()).isNull()
        assertThat(cards.findPostCardWithPosts()).isNull()
        assertThat(cards.findNextPostCard()).isNotNull
    }

    @Test
    fun `given no blogging prompt and posts, next post card is not visible and prompt card is visible`() {
        val cards = buildDashboardCards(hasBlogginPrompt = false, hasPostsForPostCard = true)

        assertThat(cards.findBloggingPromptCard()).isNull()
        assertThat(cards.findPostCardWithPosts()).isNotNull
        assertThat(cards.findNextPostCard()).isNull()
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

    private fun DashboardCards.findPostCardWithPosts() =
            this.cards.find { it is PostCardWithPostItems } as? PostCardWithPostItems

    private fun DashboardCards.findNextPostCard() =
            this.cards.find { it is PostCardWithoutPostItems } as? PostCardWithoutPostItems

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

    private fun createPostPromptCards() = listOf(
            PostCardWithoutPostItems(
                    postCardType = CREATE_FIRST,
                    title = UiStringText(""),
                    excerpt = UiStringText(""),
                    imageRes = 0,
                    footerLink = FooterLink(UiStringText(""), onClick = mock()),
                    onClick = mock()
            )
    )

    private fun buildDashboardCards(
        hasTodaysStats: Boolean = false,
        hasPostsForPostCard: Boolean = false,
        hasBlogginPrompt: Boolean = false,
        showErrorCard: Boolean = false
    ): DashboardCards {
        doAnswer { if (hasTodaysStats) todaysStatsCard else null }.whenever(todaysStatsCardBuilder).build(any())
        doAnswer { if (hasPostsForPostCard) createPostCards() else createPostPromptCards() }.whenever(postCardBuilder)
                .build(any())
        doAnswer { if (hasBlogginPrompt) blogingPromptCard else null }.whenever(bloggingPromptCardsBuilder).build(any())
        return cardsBuilder.build(
                dashboardCardsBuilderParams = DashboardCardsBuilderParams(
                        showErrorCard = showErrorCard,
                        onErrorRetryClick = { },
                        todaysStatsCardBuilderParams = TodaysStatsCardBuilderParams(mock(), mock(), mock(), mock()),
                        postCardBuilderParams = PostCardBuilderParams(mock(), mock(), mock()),
                        bloggingPromptCardBuilderParams = BloggingPromptCardBuilderParams(
                                mock(), false, mock(), mock(), mock()
                        )
                )
        )
    }
}
