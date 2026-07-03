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
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.util.toDateRangeString
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

/**
 * Backs [MostViewedDetailActivity] when it self-fetches its data. The cards these sources belong to
 * only show the first N items, so the detail screen re-requests the full, unbounded list (max = 0)
 * itself — via [MostViewedDetailFetcher] — instead of receiving it through the Intent, which could
 * exceed the Binder transaction limit for sources with many entries.
 */
@HiltViewModel
class MostViewedDetailViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val detailFetcher: MostViewedDetailFetcher,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<MostViewedDetailUiState>(MostViewedDetailUiState.Loading)
    val uiState: StateFlow<MostViewedDetailUiState> = _uiState.asStateFlow()

    private var source: MostViewedDetailSource? = null
    private var period: StatsPeriod? = null
    private var hasStartedLoading = false

    /**
     * Loads the detail data for [source] and [period]. Safe to call on every
     * [MostViewedDetailActivity] creation: the fetch only runs once (subsequent calls after e.g. a
     * rotation are ignored while a result is already present). Use [retry] to force a reload.
     */
    fun load(source: MostViewedDetailSource, period: StatsPeriod) {
        this.source = source
        this.period = period
        if (hasStartedLoading) return
        fetch()
    }

    fun retry() = fetch()

    private fun fetch() {
        val source = source ?: return
        val period = period ?: return
        val site = selectedSiteRepository.getSelectedSite()
        val accessToken = accountStore.accessToken
        if (site == null || accessToken.isNullOrEmpty()) {
            _uiState.value = MostViewedDetailUiState.Error(resourceProvider.getString(R.string.stats_error_api))
            return
        }
        hasStartedLoading = true
        _uiState.value = MostViewedDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = fetchDetail(source, site.siteId, period, accessToken)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchDetail(
        source: MostViewedDetailSource,
        siteId: Long,
        period: StatsPeriod,
        accessToken: String
    ): MostViewedDetailUiState =
        try {
            when (val result = detailFetcher.fetch(source, siteId, period, accessToken)) {
                is StatsCardFetchResult.Success -> MostViewedDetailUiState.Loaded(
                    items = result.items,
                    maxViewsForBar = result.items.firstOrNull()?.views ?: 1L,
                    totalViews = result.totalValue,
                    totalViewsChange = result.totalValueChange,
                    totalViewsChangePercent = result.totalValueChangePercent,
                    dateRange = period.toDateRangeString(resourceProvider)
                )
                is StatsCardFetchResult.Error -> MostViewedDetailUiState.Error(
                    resourceProvider.getString(result.messageResId)
                )
            }
        } catch (e: Exception) {
            MostViewedDetailUiState.Error(
                e.message ?: resourceProvider.getString(R.string.stats_error_unknown)
            )
        }
}

sealed interface MostViewedDetailUiState {
    data object Loading : MostViewedDetailUiState

    data class Loaded(
        val items: List<MostViewedDetailItem>,
        val maxViewsForBar: Long,
        val totalViews: Long,
        val totalViewsChange: Long,
        val totalViewsChangePercent: Double,
        val dateRange: String
    ) : MostViewedDetailUiState

    data class Error(val message: String) : MostViewedDetailUiState
}
