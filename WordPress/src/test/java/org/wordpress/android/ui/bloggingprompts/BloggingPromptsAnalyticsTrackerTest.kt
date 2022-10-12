package org.wordpress.android.ui.bloggingprompts

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_PROMPTS_LIST_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_PROMPTS_LIST_TAB_CHANGED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class BloggingPromptsAnalyticsTrackerTest : BaseUnitTest() {
    private val wrapper: AnalyticsTrackerWrapper = mock()
    private val site: SiteModel = mock()

    private val analyticsTracker = BloggingPromptsAnalyticsTracker(
            analyticsTracker = wrapper
    )

    @Test
    fun `Should convert prompt section selected to lowercase when tracking screen accessed`() {
        val currentTab = PromptSection.NOT_ANSWERED
        analyticsTracker.trackScreenAccessed(site, currentTab)

        verify(wrapper).track(
                BLOGGING_PROMPTS_LIST_ACCESSED,
                site,
                mutableMapOf(TRACKS_SELECTED_TAB to "not_answered")
        )
    }

    @Test
    fun `Should track tab changed`() {
        val currentTab = PromptSection.ALL
        analyticsTracker.trackTabSelected(site, currentTab)

        verify(wrapper).track(
                BLOGGING_PROMPTS_LIST_TAB_CHANGED,
                site,
                mutableMapOf(TRACKS_SELECTED_TAB to "all")
        )
    }
}
