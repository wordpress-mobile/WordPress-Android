package org.wordpress.android.ui.mysite.cards.dashboard

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
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
import org.wordpress.android.ui.quickstart.QuickStartType.NewSiteQuickStartType
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner::class)
class CardsShownTrackerTest {
    @Mock
    lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private lateinit var cardsShownTracker: CardsShownTracker

    @Before
    fun setUp() {
        cardsShownTracker = CardsShownTracker(analyticsTracker)
    }

    @Test
    fun `when quick start card is shown on dashboard tab, then quick start card shown event is tracked`() {
        cardsShownTracker.trackQuickStartCardShown(NewSiteQuickStartType)

        verifyQuickStartCardShownTracked(Type.QUICK_START.label, "quick_start_${NewSiteQuickStartType.trackingLabel}")
    }

    @Test
    fun `when post card create first card is shown, then create first shown event is tracked`() {
        cardsShownTracker.track(buildDashboardCards(PostCardType.CREATE_FIRST))

        verifyCardShownTracked(Type.POST.label, PostSubtype.CREATE_FIRST.label)
    }

    @Test
    fun `when post card create next card is shown, then create next shown event is tracked`() {
        cardsShownTracker.track(buildDashboardCards(PostCardType.CREATE_NEXT))

        verifyCardShownTracked(Type.POST.label, PostSubtype.CREATE_NEXT.label)
    }

    @Test
    fun `when post card scheduled card is shown, then scheduled shown event is tracked`() {
        cardsShownTracker.track(buildDashboardCards(PostCardType.SCHEDULED))

        verifyCardShownTracked(Type.POST.label, PostSubtype.SCHEDULED.label)
    }

    @Test
    fun `when post card drafts card is shown, then drafts shown event is tracked`() {
        cardsShownTracker.track(buildDashboardCards(PostCardType.DRAFT))

        verifyCardShownTracked(Type.POST.label, PostSubtype.DRAFT.label)
    }

    @Test
    fun `when error card is shown, then error card shown event is tracked`() {
        cardsShownTracker.track(buildErrorCard())

        verify(analyticsTracker).track(
            Stat.MY_SITE_DASHBOARD_CARD_SHOWN,
            mapOf(CardsTracker.TYPE to Type.ERROR.label, CardsTracker.SUBTYPE to Type.ERROR.label)
        )
    }

    private fun verifyCardShownTracked(type: String, subtype: String) {
        verify(analyticsTracker).track(
            Stat.MY_SITE_DASHBOARD_CARD_SHOWN,
            mapOf(CardsTracker.TYPE to type, CardsTracker.SUBTYPE to subtype)
        )
    }

    private fun verifyQuickStartCardShownTracked(type: String, subtype: String) {
        verify(analyticsTracker).track(
            Stat.MY_SITE_DASHBOARD_CARD_SHOWN,
            mapOf(CardsTracker.TYPE to type, CardsTracker.SUBTYPE to subtype)
        )
    }

    private fun buildDashboardCards(postCardType: PostCardType) = DashboardCards(
        cards = mutableListOf<DashboardCard>().apply {
            when (postCardType) {
                PostCardType.SCHEDULED, PostCardType.DRAFT -> addAll(buildPostCardsWithItems(postCardType))
                PostCardType.CREATE_FIRST, PostCardType.CREATE_NEXT -> addAll(
                    buildPostCardsWithoutItems(
                        postCardType
                    )
                )
            }
        }
    )

    private fun buildPostCardsWithoutItems(postCardType: PostCardType) =
        listOf(
            PostCardWithoutPostItems(
                postCardType = postCardType,
                title = UiStringText(""),
                footerLink = FooterLink(UiStringText(""), onClick = mock()),
                excerpt = UiStringText(""),
                imageRes = 0,
                onClick = mock()
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
        val cards = listOf(ErrorCard(onRetryClick = mock()))
        val dashboardCard = mutableListOf<DashboardCard>()
        dashboardCard.addAll(cards)
        return DashboardCards(cards = cards)
    }
}
