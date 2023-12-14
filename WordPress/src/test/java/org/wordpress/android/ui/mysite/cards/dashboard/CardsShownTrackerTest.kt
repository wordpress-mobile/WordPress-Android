package org.wordpress.android.ui.mysite.cards.dashboard

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.ActivityCard.ActivityCardWithItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.ActivityLogSubtype
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

    @Test
    fun `when activity card is shown, then activity log shown event is tracked`() {
        cardsShownTracker.track(buildActivityDashboardCards())

        verifyCardShownTracked(Type.ACTIVITY.label, ActivityLogSubtype.ACTIVITY_LOG.label)
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

    private fun buildDashboardCards(postCardType: PostCardType) =
        mutableListOf<Card>().apply {
            when (postCardType) {
                PostCardType.SCHEDULED, PostCardType.DRAFT -> addAll(buildPostCardsWithItems(postCardType))
            }
        }

    private fun buildPostCardsWithItems(postCardType: PostCardType) = listOf(
        PostCardWithPostItems(
            postCardType = postCardType,
            title = UiStringText(""),
            postItems = emptyList(),
            moreMenuResId = 0,
            moreMenuOptions = mock()
        )
    )

    private fun buildActivityDashboardCards() = mutableListOf<Card>().apply { addAll(buildActivityCard()) }

    private fun buildActivityCard() =
        listOf(
            ActivityCardWithItems(
                title = UiStringText("title"),
                onAllActivityMenuItemClick = mock(),
                onHideMenuItemClick = mock(),
                onMoreMenuClick = mock(),
                activityItems = emptyList()
            )
        )

    private fun buildErrorCard() = listOf(ErrorCard(onRetryClick = mock()))
}
