package org.wordpress.android.ui.bloggingprompts

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

@HiltViewModel
class BloggingPromptsParentViewModel @Inject constructor(
    private val handle: BloggingPromptsSiteProvider,
    private val analyticsTracker: BloggingPromptsAnalyticsTracker,
) : ViewModel() {
    fun start(site: SiteModel) {
        handle.setSite(site)
    }

    fun onOpen(currentTab: PromptSection) {
        analyticsTracker.trackScreenAccessed(getSite(), currentTab)
    }

    fun onSectionSelected(currentTab: PromptSection) {
        analyticsTracker.trackTabSelected(getSite(), currentTab)
    }

    @VisibleForTesting
    internal fun getSite(): SiteModel = checkNotNull(handle.getSite()) {
        "${WordPress.SITE} argument cannot be null"
    }
}
