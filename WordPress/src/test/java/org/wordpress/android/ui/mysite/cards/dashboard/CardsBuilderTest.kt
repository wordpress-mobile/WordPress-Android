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
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.FooterLink
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardBuilder
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType.DRAFT
import org.wordpress.android.ui.utils.UiString.UiStringText

@RunWith(MockitoJUnitRunner::class)
class CardsBuilderTest : BaseUnitTest() {
    @Mock lateinit var postCardBuilder: PostCardBuilder
    private lateinit var cardsBuilder: CardsBuilder

    @Before
    fun setUp() {
        cardsBuilder = CardsBuilder(postCardBuilder)
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

    private fun DashboardCards.findPostCard() = this.cards.find { it is PostCard } as? PostCard

    private fun DashboardCards.findErrorCard() = this.cards.find { it is ErrorCard } as? ErrorCard

    private fun createPostCards() = listOf(
            PostCardWithPostItems(
                    postCardType = DRAFT,
                    title = UiStringText(""),
                    postItems = emptyList(),
                    footerLink = FooterLink(UiStringText(""), onClick = mock())
            )
    )

    private fun buildDashboardCards(
        hasPosts: Boolean = false,
        showErrorCard: Boolean = false
    ): DashboardCards {
        doAnswer { if (hasPosts) createPostCards() else emptyList() }.whenever(postCardBuilder).build(any())
        return cardsBuilder.build(
                dashboardCardsBuilderParams = DashboardCardsBuilderParams(
                        showErrorCard = showErrorCard,
                        onErrorRetryClick = { },
                        postCardBuilderParams = PostCardBuilderParams(mock(), mock(), mock())
                )
        )
    }
}
