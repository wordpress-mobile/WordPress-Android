package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.InsightsUiState.StatsListState.DONE
import org.wordpress.android.ui.stats.refresh.usecases.InsightsUseCase
import org.wordpress.android.util.map
import javax.inject.Inject
import javax.inject.Named

class InsightsTabViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    val insightsUseCase: InsightsUseCase
) : StatsListViewModel(mainDispatcher) {
    private val _data = insightsUseCase.data.map { loadedData ->
        InsightsUiState(loadedData, DONE)
    }
    override val data: LiveData<InsightsUiState> = _data

    override val navigationTarget: LiveData<NavigationTarget> = insightsUseCase.navigationTarget

    override fun onCleared() {
        super.onCleared()
        insightsUseCase.onCleared()
    }
}
