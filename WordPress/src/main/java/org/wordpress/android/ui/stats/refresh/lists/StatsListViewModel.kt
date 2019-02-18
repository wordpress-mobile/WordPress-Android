package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.LiveData
import android.support.annotation.StringRes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.throttle
import org.wordpress.android.viewmodel.ScopedViewModel

const val SCROLL_EVENT_DELAY = 2000L

abstract class StatsListViewModel(
    defaultDispatcher: CoroutineDispatcher,
    private val statsUseCase: BaseListUseCase,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(defaultDispatcher) {
    private var trackJob: Job? = null

    enum class StatsSection(@StringRes val titleRes: Int) {
        INSIGHTS(R.string.stats_insights),
        DAYS(R.string.stats_timeframe_days),
        WEEKS(R.string.stats_timeframe_weeks),
        MONTHS(R.string.stats_timeframe_months),
        YEARS(R.string.stats_timeframe_years);
    }

    val navigationTarget: LiveData<NavigationTarget> = statsUseCase.navigationTarget

    val data: LiveData<List<StatsBlock>> by lazy { statsUseCase.data.throttle(this) }

    override fun onCleared() {
        statsUseCase.onCleared()
        super.onCleared()
    }

    fun onScrolledToBottom() {
        if (trackJob?.isCompleted != false) {
            trackJob = launch {
                analyticsTracker.track(AnalyticsTracker.Stat.STATS_SCROLLED_TO_BOTTOM)
                delay(SCROLL_EVENT_DELAY)
            }
        }
    }
}
