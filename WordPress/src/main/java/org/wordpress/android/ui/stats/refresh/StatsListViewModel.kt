package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.support.annotation.StringRes
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.android.HandlerDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.viewmodel.ScopedViewModel

abstract class StatsListViewModel(defaultDispatcher: HandlerDispatcher) : ScopedViewModel(defaultDispatcher) {
    enum class StatsListType(@StringRes val titleRes: Int) {
        INSIGHTS(R.string.stats_insights),
        DAYS(R.string.stats_timeframe_days),
        WEEKS(R.string.stats_timeframe_weeks),
        MONTHS(R.string.stats_timeframe_months);
    }

    abstract val data: LiveData<InsightsUiState>

    abstract fun start(site: SiteModel)

    abstract fun reload(site: SiteModel)
}

data class InsightsUiState(val data: List<InsightsItem> = listOf(), val status: StatsListState) {
    enum class StatsListState {
        DONE,
        ERROR,
        FETCHING
    }
}
