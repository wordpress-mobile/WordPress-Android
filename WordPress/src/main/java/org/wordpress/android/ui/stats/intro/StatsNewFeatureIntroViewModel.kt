package org.wordpress.android.ui.stats.intro

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.intro.StatsNewFeaturesIntroAction.DismissDialog
import org.wordpress.android.ui.stats.intro.StatsNewFeaturesIntroAction.OpenStats
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class StatsNewFeatureIntroViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val statsSiteProvider: StatsSiteProvider,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _action = MutableLiveData<StatsNewFeaturesIntroAction>()
    val action: LiveData<StatsNewFeaturesIntroAction> = _action

    fun start() {
        analyticsTracker.track(Stat.STATS_REVAMP_V2_ANNOUNCEMENT_SHOWN)
    }

    fun onPrimaryButtonClick() = launch {
        analyticsTracker.track(Stat.STATS_REVAMP_V2_ANNOUNCEMENT_CONFIRMED)
        appPrefsWrapper.markStatsRevampFeatureAnnouncementAsDisplayed()
        _action.postValue(OpenStats(statsSiteProvider.siteModel))
    }

    fun onSecondaryButtonClick() {
        analyticsTracker.track(Stat.STATS_REVAMP_V2_ANNOUNCEMENT_DISMISSED)
        _action.value = DismissDialog
    }
}
