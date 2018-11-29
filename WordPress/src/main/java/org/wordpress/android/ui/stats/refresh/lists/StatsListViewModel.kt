package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.LiveData
import android.support.annotation.StringRes
import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.StatsUiState.StatsListState.DONE
import org.wordpress.android.util.map
import org.wordpress.android.viewmodel.ScopedViewModel

abstract class StatsListViewModel(defaultDispatcher: CoroutineDispatcher, private val statsUseCase: BaseListUseCase) :
        ScopedViewModel(defaultDispatcher) {
    private val _data = statsUseCase.data.map { loadedData ->
        StatsUiState(loadedData, DONE)
    }

    enum class StatsListType(@StringRes val titleRes: Int) {
        INSIGHTS(R.string.stats_insights),
        DAYS(R.string.stats_timeframe_days),
        WEEKS(R.string.stats_timeframe_weeks),
        MONTHS(R.string.stats_timeframe_months),
        YEARS(R.string.stats_timeframe_years);
    }

    val navigationTarget: LiveData<NavigationTarget> = statsUseCase.navigationTarget

    val data: LiveData<StatsUiState> = _data

    override fun onCleared() {
        statsUseCase.onCleared()
        super.onCleared()
    }
}

data class StatsUiState(val data: List<StatsBlock> = listOf(), val status: StatsListState) {
    enum class StatsListState {
        DONE,
        ERROR,
        FETCHING
    }
}
