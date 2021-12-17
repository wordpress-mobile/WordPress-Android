package org.wordpress.android.ui.mysite.cards.dashboard

import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.cards.dashboard.DashboardCardsTracker.DashboardCardPropertySubtype
import org.wordpress.android.ui.mysite.cards.dashboard.DashboardCardsTracker.DashboardCardPropertyType
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

private const val TYPE = "type"
private const val SUBTYPE = "subtype"

@RunWith(MockitoJUnitRunner::class)
class DashboardCardsTrackerTest {
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private lateinit var dashboardCardsTracker: DashboardCardsTracker

    @Before
    fun setUp() {
        dashboardCardsTracker = DashboardCardsTracker(analyticsTracker)
    }

    @Test
    fun `when post create first footer link is clicked, then post create first event is tracked`() {
        dashboardCardsTracker.trackDashboardPostCardFooterLinkClicked(PostCardType.CREATE_FIRST)

        verifyFooterLinkClickedTracked(DashboardCardPropertyType.POST, DashboardCardPropertySubtype.CREATE_FIRST)
    }

    @Test
    fun `when post create next footer link is clicked, then post create next event is tracked`() {
        dashboardCardsTracker.trackDashboardPostCardFooterLinkClicked(PostCardType.CREATE_NEXT)

        verifyFooterLinkClickedTracked(DashboardCardPropertyType.POST, DashboardCardPropertySubtype.CREATE_NEXT)
    }

    @Test
    fun `when post draft footer link is clicked, then post draft event is tracked`() {
        dashboardCardsTracker.trackDashboardPostCardFooterLinkClicked(PostCardType.DRAFT)

        verifyFooterLinkClickedTracked(DashboardCardPropertyType.POST, DashboardCardPropertySubtype.DRAFT)
    }

    @Test
    fun `when post scheduled footer link is clicked, then post scheduled event is tracked`() {
        dashboardCardsTracker.trackDashboardPostCardFooterLinkClicked(PostCardType.SCHEDULED)

        verifyFooterLinkClickedTracked(DashboardCardPropertyType.POST, DashboardCardPropertySubtype.SCHEDULED)
    }

    private fun verifyFooterLinkClickedTracked(
        typeValue: DashboardCardPropertyType,
        subtypeValue: DashboardCardPropertySubtype
    ) {
        verify(analyticsTracker).track(
                Stat.MY_SITE_DASHBOARD_CARD_FOOTER_ACTION_TAPPED,
                mapOf(TYPE to typeValue.label, SUBTYPE to subtypeValue.label)
        )
    }
}
