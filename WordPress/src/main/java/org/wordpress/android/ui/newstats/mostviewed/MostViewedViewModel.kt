package org.wordpress.android.ui.newstats.mostviewed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.MostViewedResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class MostViewedViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<MostViewedCardUiState>(MostViewedCardUiState.Loading)
    val uiState: StateFlow<MostViewedCardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentDataSource: MostViewedDataSource = MostViewedDataSource.POSTS_AND_PAGES

    init {
        loadData()
    }

    fun onDataSourceChanged(dataSource: MostViewedDataSource) {
        if (dataSource == currentDataSource) return
        currentDataSource = dataSource
        loadData()
    }

    fun refresh() {
        val site = selectedSiteRepository.getSelectedSite() ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            loadDataInternal(site.siteId)
            _isRefreshing.value = false
        }
    }

    fun onRetry() {
        loadData()
    }

    private fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            _uiState.value = MostViewedCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_todays_stats_no_site_selected)
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            _uiState.value = MostViewedCardUiState.Error(
                message = resourceProvider.getString(R.string.stats_todays_stats_failed_to_load)
            )
            return
        }

        statsRepository.init(accessToken)
        _uiState.value = MostViewedCardUiState.Loading

        viewModelScope.launch {
            loadDataInternal(site.siteId)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadDataInternal(siteId: Long) {
        try {
            val result = statsRepository.fetchMostViewed(siteId, currentDataSource)

            when (result) {
                is MostViewedResult.Success -> {
                    _uiState.value = MostViewedCardUiState.Loaded(
                        selectedDataSource = currentDataSource,
                        items = result.items.map { item ->
                            MostViewedItem(
                                id = item.id,
                                title = item.title,
                                views = item.views,
                                change = MostViewedChange.NotAvailable,
                                isHighlighted = item.isFirst
                            )
                        }
                    )
                }
                is MostViewedResult.Error -> {
                    _uiState.value = MostViewedCardUiState.Error(
                        message = resourceProvider.getString(R.string.stats_todays_stats_failed_to_load)
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.value = MostViewedCardUiState.Error(
                message = e.message
                    ?: resourceProvider.getString(R.string.stats_todays_stats_unknown_error)
            )
        }
    }
}
