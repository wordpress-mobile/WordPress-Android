package org.wordpress.android.ui.stats.refresh.lists.sections.dwmy

import android.arch.lifecycle.LiveData
import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.DAY_STATS_USE_CASE
import org.wordpress.android.ui.stats.refresh.lists.BaseListUseCase
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import org.wordpress.android.ui.stats.refresh.lists.StatsUiState
import org.wordpress.android.ui.stats.refresh.lists.StatsUiState.StatsListState.DONE
import org.wordpress.android.util.map
import javax.inject.Inject
import javax.inject.Named

class DaysListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(DAY_STATS_USE_CASE) private val dayStatsUseCase: BaseListUseCase
) : StatsListViewModel(mainDispatcher) {
    private val _data = dayStatsUseCase.data.map { loadedData ->
        StatsUiState(loadedData, DONE)
    }
    override val data: LiveData<StatsUiState> = _data

    override val navigationTarget: LiveData<NavigationTarget> = dayStatsUseCase.navigationTarget

    override fun onCleared() {
        dayStatsUseCase.onCleared()
        super.onCleared()
    }
}
