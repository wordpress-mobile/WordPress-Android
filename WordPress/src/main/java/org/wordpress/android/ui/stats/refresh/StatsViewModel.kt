package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.DEFAULT_SCOPE
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.stats.refresh.InsightsUiState.ListStatus
import org.wordpress.android.ui.stats.refresh.InsightsUiState.ListStatus.FETCHING
import org.wordpress.android.ui.stats.refresh.StatsListViewModel.StatsListType
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class StatsViewModel
@Inject constructor(
    private val insightsDomain: InsightsDomain,
    private val dispatcher: Dispatcher,
    @Named(UI_SCOPE) private val uiScope: CoroutineScope,
    @Named(DEFAULT_SCOPE) private val defaultScope: CoroutineScope
) : ViewModel() {
    private val _listState = MutableLiveData<ListStatus>()
    val listState: LiveData<ListStatus> = _listState

    private var isInitialized = false

    private val _showSnackbarMessage = SingleLiveEvent<SnackbarMessageHolder>()
    val showSnackbarMessage: LiveData<SnackbarMessageHolder> = _showSnackbarMessage

    private var _site: SiteModel? = null
    val site: SiteModel
        get() = checkNotNull(_site) { "Trying to access unitialized site" }

    private var currentStatsType = StatsListType.INSIGHTS

    fun start(site: SiteModel) {
        // Check if VM is not already initialized
        if (_site == null) {
            _site = site

            loadStats()
        }
    }

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        dispatcher.unregister(this)
    }

    private fun loadStats() = defaultScope.launch {
        val loadState = FETCHING
        reloadStats(loadState)

        isInitialized = true
    }

    private suspend fun reloadStats(state: ListStatus = FETCHING) {
        _listState.setOnUi(state)

        val result = insightsDomain.loadInsightItems()
    }

    fun onMenuAction(action: Action, page: Page): Boolean {
        return when (action) {
            else -> true
        }
    }

    fun onItemTapped(pageItem: Page) {
    }


    fun onPullToRefresh() {
        uiScope.launch {
            reloadStats()
        }
    }

    private suspend fun <T> MutableLiveData<T>.setOnUi(value: T) = withContext(uiScope.coroutineContext) {
        setValue(value)
    }

    private fun <T> MutableLiveData<T>.postOnUi(value: T) {
        val liveData = this
        uiScope.launch {
            liveData.value = value
        }
    }
}
