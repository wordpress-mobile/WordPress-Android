package org.wordpress.android.ui.mysite.cards.dashboard

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.PostSubtype
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.QuickStartSubtype
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.StatsSubtype
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.Type
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.ui.quickstart.QuickStartTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

private const val TYPE = "type"
private const val SUBTYPE = "subtype"

@RunWith(MockitoJUnitRunner::class)
class CardsTrackerTest {
    @Mock
    lateinit var analyticsTracker: AnalyticsTrackerWrapper
    @Mock
    lateinit var cardsShownTracker: CardsShownTracker
    @Mock
    lateinit var quickStartTracker: QuickStartTracker
    private lateinit var cardsTracker: CardsTracker

    @Before
    fun setUp() {
        cardsTracker = CardsTracker(cardsShownTracker, analyticsTracker, quickStartTracker)
    }

    /* QUICK START CARD */

    @Test
    fun `when quick start card grow item is clicked, then quick start card item tapped event is tracked`() {
        cardsTracker.trackQuickStartCardItemClicked(QuickStartTaskType.GROW)

        verifyQuickStartCardItemClickedTracked(QuickStartSubtype.GROW.label)
    }

    @Test
    fun `when quick start card customize item is clicked, then quick start card item tapped event is tracked`() {
        cardsTracker.trackQuickStartCardItemClicked(QuickStartTaskType.CUSTOMIZE)

        verifyQuickStartCardItemClickedTracked(QuickStartSubtype.CUSTOMIZE.label)
    }

    /* TODAY'S STATS CARD */

    @Test
    fun `when today's stats card get more views link is clicked, then today's stats nudge event is tracked`() {
        cardsTracker.trackTodaysStatsCardGetMoreViewsNudgeClicked()

        verifyCardItemClickedTracked(Type.STATS, StatsSubtype.TODAYS_STATS_NUDGE.label)
    }

    @Test
    fun `when today's stats card footer link is clicked, then today's stats card footer click event is tracked`() {
        cardsTracker.trackTodaysStatsCardFooterLinkClicked()

        verifyFooterLinkClickedTracked(Type.STATS, StatsSubtype.TODAYS_STATS.label)
    }

    @Test
    fun `when today's stats card is clicked, then today's stats card item click event is tracked`() {
        cardsTracker.trackTodaysStatsCardClicked()

        verifyCardItemClickedTracked(Type.STATS, StatsSubtype.TODAYS_STATS.label)
    }

    /* POST CARDS */

    @Test
    fun `when post create first footer link is clicked, then post create first event is tracked`() {
        cardsTracker.trackPostCardFooterLinkClicked(PostCardType.CREATE_FIRST)

        verifyFooterLinkClickedTracked(Type.POST, PostSubtype.CREATE_FIRST.label)
    }

    @Test
    fun `when post create next footer link is clicked, then post create next event is tracked`() {
        cardsTracker.trackPostCardFooterLinkClicked(PostCardType.CREATE_NEXT)

        verifyFooterLinkClickedTracked(Type.POST, PostSubtype.CREATE_NEXT.label)
    }

    @Test
    fun `when post draft footer link is clicked, then post draft event is tracked`() {
        cardsTracker.trackPostCardFooterLinkClicked(PostCardType.DRAFT)

        verifyFooterLinkClickedTracked(Type.POST, PostSubtype.DRAFT.label)
    }

    @Test
    fun `when post scheduled footer link is clicked, then post scheduled event is tracked`() {
        cardsTracker.trackPostCardFooterLinkClicked(PostCardType.SCHEDULED)

        verifyFooterLinkClickedTracked(Type.POST, PostSubtype.SCHEDULED.label)
    }

    @Test
    fun `when post draft item is clicked, then post item event is tracked`() {
        cardsTracker.trackPostItemClicked(PostCardType.DRAFT)

        verifyCardItemClickedTracked(Type.POST, PostSubtype.DRAFT.label)
    }

    @Test
    fun `when post scheduled item is clicked, then post item event is tracked`() {
        cardsTracker.trackPostItemClicked(PostCardType.SCHEDULED)

        verifyCardItemClickedTracked(Type.POST, PostSubtype.SCHEDULED.label)
    }

    private fun verifyFooterLinkClickedTracked(
        typeValue: Type,
        subtypeValue: String
    ) {
        verify(analyticsTracker).track(
            Stat.MY_SITE_DASHBOARD_CARD_FOOTER_ACTION_TAPPED,
            mapOf(TYPE to typeValue.label, SUBTYPE to subtypeValue)
        )
    }

    private fun verifyCardItemClickedTracked(
        typeValue: Type,
        subtypeValue: String
    ) {
        verify(analyticsTracker).track(
            Stat.MY_SITE_DASHBOARD_CARD_ITEM_TAPPED,
            mapOf(TYPE to typeValue.label, SUBTYPE to subtypeValue)
        )
    }

    private fun verifyQuickStartCardItemClickedTracked(
        subtypeValue: String
    ) {
        verify(quickStartTracker).track(
            Stat.MY_SITE_DASHBOARD_CARD_ITEM_TAPPED,
            mapOf(TYPE to Type.QUICK_START.label, SUBTYPE to subtypeValue)
        )
    }
}
