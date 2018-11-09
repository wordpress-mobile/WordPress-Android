package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.InsightsUiState.StatsListState.DONE
import javax.inject.Inject
import javax.inject.Named

class DaysTabViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher
) : StatsListViewModel(mainDispatcher) {
    private val _data = MutableLiveData<InsightsUiState>()
    override val data: LiveData<InsightsUiState> = _data

    init {
        _data.value = InsightsUiState(listOf(Empty()), DONE)
    }
}
