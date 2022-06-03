package org.wordpress.android.ui.stats.intro

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.intro.StatsNewFeaturesIntroAction.DismissDialog
import org.wordpress.android.ui.stats.intro.StatsNewFeaturesIntroAction.OpenStats
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class StatsNewFeatureIntroViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val statsSiteProvider: StatsSiteProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _action = MutableLiveData<StatsNewFeaturesIntroAction>()
    val action: LiveData<StatsNewFeaturesIntroAction> = _action

    fun onPrimaryButtonClick() = launch {
        analyticsTracker.track(Stat.STATS_REVAMP_V2_INTRO_TRY_IT_NOW_CLICKED)
        _action.postValue(OpenStats(statsSiteProvider.siteModel))
    }

    fun onSecondaryButtonClick() {
        analyticsTracker.track(Stat.STATS_REVAMP_V2_INTRO_REMIND_ME_CLICKED)
        _action.value = DismissDialog
    }
}
