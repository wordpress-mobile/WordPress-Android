package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModelProvider
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.ui.stats.refresh.InsightsUiState.StatsListState.DONE
import javax.inject.Inject
import javax.inject.Named

class DaysTabViewModel @Inject constructor(
    private val insightsViewModel: InsightsViewModel,
    @Named(UI_SCOPE) private val uiScope: CoroutineScope
) : StatsListViewModel() {
    private val _data = Transformations.map(insightsViewModel.data) {
        InsightsUiState(listOf(Empty()), DONE)
    }
    override val data: LiveData<InsightsUiState> = _data

    override fun start(site: SiteModel) {
        reload(site)
    }

    override fun reload(site: SiteModel) {
        uiScope.launch {
            insightsViewModel.loadInsightItems(site, false)
        }
    }
}
