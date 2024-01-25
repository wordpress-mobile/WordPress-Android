package org.wordpress.android.ui.sitemonitor

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SiteMonitorParentViewModel @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(bgDispatcher) {
    private lateinit var site: SiteModel

    fun start(site: SiteModel) {
        this.site = site
        trackActivityLaunched()
    }

    private fun trackActivityLaunched() {
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.SITE_MONITORING_SCREEN_SHOWN)
    }
}
