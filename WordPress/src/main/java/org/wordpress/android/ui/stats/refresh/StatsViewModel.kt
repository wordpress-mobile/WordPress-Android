package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.android.HandlerDispatcher
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.DEFAULT_SCOPE
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stats.refresh.InsightsUiState.StatsListState
import org.wordpress.android.ui.stats.refresh.InsightsUiState.StatsListState.DONE
import org.wordpress.android.ui.stats.refresh.InsightsUiState.StatsListState.FETCHING
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class StatsViewModel
@Inject constructor(
    private val insightsUseCase: InsightsUseCase,
    @Named(UI_THREAD) private val mainDispatcher: HandlerDispatcher
) : ScopedViewModel(mainDispatcher) {
    private lateinit var site: SiteModel

    private val _listState = MutableLiveData<StatsListState>()
    val listState: LiveData<StatsListState> = _listState

    private var isInitialized = false

    private val _showSnackbarMessage = SingleLiveEvent<SnackbarMessageHolder>()
    val showSnackbarMessage: LiveData<SnackbarMessageHolder> = _showSnackbarMessage

    fun start(site: SiteModel) {
        // Check if VM is not already initialized
        if (!isInitialized) {
            isInitialized = true

            this.site = site
            this.insightsUseCase.reset()

            reloadStats()
        }
    }

    private fun CoroutineScope.reloadStats() = launch {
        _listState.value = FETCHING

        insightsUseCase.loadInsightItems(site)

        _listState.value = DONE
    }

    // TODO: To be implemented in the future
    fun onMenuAction(action: Action, page: Page): Boolean {
        return when (action) {
            else -> true
        }
    }

    // TODO: To be implemented in the future
    fun onItemTapped(pageItem: Page) {
    }

    fun onPullToRefresh() {
        reloadStats()
    }
}
