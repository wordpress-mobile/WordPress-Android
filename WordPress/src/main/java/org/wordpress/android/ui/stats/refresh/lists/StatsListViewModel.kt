package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.LiveData
import android.support.annotation.StringRes
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.viewmodel.ScopedViewModel

abstract class StatsListViewModel(defaultDispatcher: CoroutineDispatcher, private val statsUseCase: BaseListUseCase) :
        ScopedViewModel(defaultDispatcher) {
    private val _data = statsUseCase.data

    enum class StatsSection(@StringRes val titleRes: Int) {
        INSIGHTS(R.string.stats_insights),
        DAYS(R.string.stats_timeframe_days),
        WEEKS(R.string.stats_timeframe_weeks),
        MONTHS(R.string.stats_timeframe_months),
        YEARS(R.string.stats_timeframe_years);
    }

    val navigationTarget: LiveData<NavigationTarget> = statsUseCase.navigationTarget

    val data: LiveData<List<StatsBlock>> = _data

    override fun onCleared() {
        statsUseCase.onCleared()
        super.onCleared()
    }
}
