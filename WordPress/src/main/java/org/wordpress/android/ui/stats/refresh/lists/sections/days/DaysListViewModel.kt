package org.wordpress.android.ui.stats.refresh.lists.sections.days

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.Empty
import org.wordpress.android.ui.stats.refresh.lists.InsightsUiState
import org.wordpress.android.ui.stats.refresh.lists.InsightsUiState.StatsListState.DONE
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import javax.inject.Inject
import javax.inject.Named

class DaysListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher
) : StatsListViewModel(mainDispatcher) {
    private val _data = MutableLiveData<InsightsUiState>()
    override val data: LiveData<InsightsUiState> = _data

    override val navigationTarget: LiveData<NavigationTarget> = MutableLiveData()

    init {
        _data.value = InsightsUiState(
                listOf(Empty()),
                DONE
        )
    }
}
