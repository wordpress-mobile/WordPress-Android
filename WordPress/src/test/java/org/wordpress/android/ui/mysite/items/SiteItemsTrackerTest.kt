package org.wordpress.android.ui.mysite.items

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.items.SiteItemsTracker.Type
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

private const val TYPE = "type"

@RunWith(MockitoJUnitRunner::class)
class SiteItemsTrackerTest {
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private lateinit var siteItemsTracker: SiteItemsTracker

    @Before
    fun setUp() {
        siteItemsTracker = SiteItemsTracker(analyticsTracker)
    }

    @Test
    fun `when site item posts is clicked, then menu item tapped posts is tracked`() {
        siteItemsTracker.trackSiteItemClicked(ListItemAction.POSTS)

        verifySiteItemClickedTracked(Type.POSTS)
    }

    @Test
    fun `given not tracking site item, when site item is clicked, then click is not tracked`() {
        siteItemsTracker.trackSiteItemClicked(ListItemAction.VIEW_SITE)

        verify(analyticsTracker, times(0)).track(any())
    }

    private fun verifySiteItemClickedTracked(typeValue: Type) {
        verify(analyticsTracker).track(Stat.MY_SITE_MENU_ITEM_TAPPED, mapOf(TYPE to typeValue.label))
    }
}
