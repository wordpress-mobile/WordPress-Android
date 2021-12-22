package org.wordpress.android.ui.mysite.cards.dashboard

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.FooterLink
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithoutPostItems
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.PostSubtype
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.Type
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

private const val TYPE = "type"
private const val SUBTYPE = "subtype"

@RunWith(MockitoJUnitRunner::class)
class CardsTrackerTest {
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private lateinit var cardsTracker: CardsTracker

    @Before
    fun setUp() {
        cardsTracker = CardsTracker(analyticsTracker)
    }

    @Test
    fun `when post create first footer link is clicked, then post create first event is tracked`() {
        cardsTracker.trackPostCardFooterLinkClicked(PostCardType.CREATE_FIRST)

        verifyFooterLinkClickedTracked(Type.POST, PostSubtype.CREATE_FIRST)
    }

    @Test
    fun `when post create next footer link is clicked, then post create next event is tracked`() {
        cardsTracker.trackPostCardFooterLinkClicked(PostCardType.CREATE_NEXT)

        verifyFooterLinkClickedTracked(Type.POST, PostSubtype.CREATE_NEXT)
    }

    @Test
    fun `when post draft footer link is clicked, then post draft event is tracked`() {
        cardsTracker.trackPostCardFooterLinkClicked(PostCardType.DRAFT)

        verifyFooterLinkClickedTracked(Type.POST, PostSubtype.DRAFT)
    }

    @Test
    fun `when post scheduled footer link is clicked, then post scheduled event is tracked`() {
        cardsTracker.trackPostCardFooterLinkClicked(PostCardType.SCHEDULED)

        verifyFooterLinkClickedTracked(Type.POST, PostSubtype.SCHEDULED)
    }

    @Test
    fun `when post card create first card is shown, then create first shown event is tracked`() {
        cardsTracker.trackCardsShown(buildDashboardCards(PostCardType.CREATE_FIRST))

        verifyCardShowTracked(Type.POST.label, PostSubtype.CREATE_FIRST.label)
    }

    @Test
    fun `when post card create next card is shown, then create next shown event is tracked`() {
        cardsTracker.trackCardsShown(buildDashboardCards(PostCardType.CREATE_NEXT))

        verifyCardShowTracked(Type.POST.label, PostSubtype.CREATE_NEXT.label)
    }

    @Test
    fun `when post card scheduled card is shown, then scheduled shown event is tracked`() {
        cardsTracker.trackCardsShown(buildDashboardCards(PostCardType.SCHEDULED))

        verifyCardShowTracked(Type.POST.label, PostSubtype.SCHEDULED.label)
    }

    @Test
    fun `when post card drafts card is shown, then drafts shown event is tracked`() {
        cardsTracker.trackCardsShown(buildDashboardCards(PostCardType.DRAFT))

        verifyCardShowTracked(Type.POST.label, PostSubtype.DRAFT.label)
    }

    @Test
    fun `when error card is shown, then error card shown event is tracked`() {
        cardsTracker.trackCardsShown(buildErrorCard())

        verify(analyticsTracker).track(
                Stat.MY_SITE_DASHBOARD_CARD_SHOWN,
                mapOf(TYPE to Type.ERROR.label)
        )
    }


    private fun verifyFooterLinkClickedTracked(
        typeValue: Type,
        subtypeValue: PostSubtype
    ) {
        verify(analyticsTracker).track(
                Stat.MY_SITE_DASHBOARD_CARD_FOOTER_ACTION_TAPPED,
                mapOf(TYPE to typeValue.label, SUBTYPE to subtypeValue.label)
        )
    }

    private fun verifyCardShowTracked(type: String, subtype: String) {
        verify(analyticsTracker).track(
                Stat.MY_SITE_DASHBOARD_CARD_SHOWN,
                mapOf(TYPE to type, SUBTYPE to subtype)
        )
    }

    private fun buildDashboardCards(postCardType: PostCardType): DashboardCards {
        val cards = when(postCardType) {
            PostCardType.SCHEDULED, PostCardType.DRAFT -> buildPostCardsWithItems(postCardType)
            PostCardType.CREATE_FIRST, PostCardType.CREATE_NEXT -> buildPostCardsWithoutItems(postCardType)
        }
        val dashboardCard = mutableListOf<DashboardCard>()
        dashboardCard.addAll(cards)
        return DashboardCards(cards = cards)
    }

    private fun buildPostCardsWithoutItems(postCardType: PostCardType) =
        listOf(PostCardWithoutPostItems(
                postCardType = postCardType,
                title = UiStringText(""),
                footerLink = FooterLink(UiStringText(""), onClick = mock()),
                excerpt = UiStringText(""),
                imageRes = 0
            )
    )

    private fun buildPostCardsWithItems(postCardType: PostCardType) = listOf(
            PostCardWithPostItems(
                    postCardType = postCardType,
                    title = UiStringText(""),
                    postItems = emptyList(),
                    footerLink = FooterLink(UiStringText(""), onClick = mock())
            )
    )

    private fun buildErrorCard(): DashboardCards {
        val cards = listOf(ErrorCard())
        val dashboardCard = mutableListOf<DashboardCard>()
        dashboardCard.addAll(cards)
        return DashboardCards(cards = cards)
    }
}
