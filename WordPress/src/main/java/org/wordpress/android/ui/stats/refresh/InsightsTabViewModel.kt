package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.ui.stats.refresh.InsightsUiState.StatsListState.DONE
import org.wordpress.android.ui.stats.refresh.InsightsUiState.StatsListState.FETCHING
import javax.inject.Inject
import javax.inject.Named

class InsightsTabViewModel @Inject constructor(
    @Named(UI_SCOPE) private val uiScope: CoroutineScope,
    private val insightsViewModel: InsightsViewModel
) : StatsListViewModel() {
    private val _data = Transformations.map(insightsViewModel.data) { loadedData ->
        InsightsUiState(loadedData, DONE)
    }
    override val data: LiveData<InsightsUiState> = _data

    override val navigationTarget: LiveData<NavigationTarget> = insightsViewModel.navigationTarget

    override fun start(site: SiteModel) {
        reload(site)
    }

    override fun reload(site: SiteModel) {
        uiScope.launch {
            insightsViewModel.loadInsightItems(site, false)
        }
    }
}
