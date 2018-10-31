package org.wordpress.android.ui.sitecreation

import android.arch.lifecycle.ViewModel
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.Dispatcher
import javax.inject.Inject

class NewSiteCreationCategoryViewModel
@Inject constructor(
    private val dispatcher: Dispatcher
) : ViewModel() {
    private var isInitialized = false

    fun start() {
        if(isInitialized)return
        isInitialized = true
        AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_CATEGORY_VIEWED)
    }

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        dispatcher.unregister(this)
    }

    // TODO fetch categories
    // TODO create categories observable
    // TODO error handling
    // TODO retry action
    // TODO user interaction - category selected
    // TODO analytics
}
