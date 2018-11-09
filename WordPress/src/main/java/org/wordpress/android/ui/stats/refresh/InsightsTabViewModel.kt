package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.InsightsUiState.StatsListState.DONE
import javax.inject.Inject
import javax.inject.Named

class InsightsTabViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    insightsUseCase: InsightsUseCase
) : StatsListViewModel(mainDispatcher) {
    private val _data = Transformations.map(insightsUseCase.data) { loadedData ->
        InsightsUiState(loadedData, DONE)
    }
    override val data: LiveData<InsightsUiState> = _data
}
