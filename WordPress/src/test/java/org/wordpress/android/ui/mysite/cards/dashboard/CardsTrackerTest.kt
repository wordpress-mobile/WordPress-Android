package org.wordpress.android.ui.mysite.cards.dashboard

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.ActivityLogSubtype
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.QuickStartSubtype
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker.Type
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

    @Test
    fun `when activity card footer link is clicked, then footer link clicked is tracked`() {
        cardsTracker.trackActivityCardFooterClicked()

        verifyFooterLinkClickedTracked(Type.ACTIVITY, ActivityLogSubtype.ACTIVITY_LOG.label)
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

    private fun verifyQuickStartCardItemClickedTracked(
        subtypeValue: String
    ) {
        verify(quickStartTracker).track(
            Stat.MY_SITE_DASHBOARD_CARD_ITEM_TAPPED,
            mapOf(TYPE to Type.QUICK_START.label, SUBTYPE to subtypeValue)
        )
    }
}
