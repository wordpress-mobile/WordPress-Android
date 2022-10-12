package org.wordpress.android.ui.bloggingprompts

import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_PROMPTS_LIST_ACCESSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.BLOGGING_PROMPTS_LIST_TAB_CHANGED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Locale
import javax.inject.Inject

class BloggingPromptsAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    fun trackScreenAccessed(site: SiteModel, currentTab: PromptSection) =
            analyticsTracker.track(
                    BLOGGING_PROMPTS_LIST_ACCESSED,
                    site,
                    mutableMapOf(TRACKS_SELECTED_TAB to currentTab.name.lowercase(Locale.US))
            )

    fun trackTabSelected(site: SiteModel, currentTab: PromptSection) =
            analyticsTracker.track(
                    BLOGGING_PROMPTS_LIST_TAB_CHANGED,
                    site,
                    mutableMapOf(TRACKS_SELECTED_TAB to currentTab.name.lowercase(Locale.US))
            )
}

internal const val TRACKS_SELECTED_TAB = "selected_tab"
