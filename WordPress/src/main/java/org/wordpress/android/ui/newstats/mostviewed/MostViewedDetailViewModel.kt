package org.wordpress.android.ui.newstats.mostviewed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.MostViewedResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.util.toDateRangeString
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

/**
 * Backs [MostViewedDetailActivity] when it self-fetches its data (currently the referrers detail
 * screen). The referrers card is bounded to [StatsRepository]'s card max, so the detail screen
 * re-requests the full, unbounded list (max = 0) instead of receiving it through the Intent — this
 * keeps the card request small and avoids passing a large parcelable list across the process
 * boundary (which could exceed the Binder transaction limit).
 */
@HiltViewModel
class MostViewedDetailViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<MostViewedDetailUiState>(MostViewedDetailUiState.Loading)
    val uiState: StateFlow<MostViewedDetailUiState> = _uiState.asStateFlow()

    private var period: StatsPeriod? = null
    private var hasStartedLoading = false

    /**
     * Loads the referrers detail data for [period]. Safe to call on every [MostViewedDetailActivity]
     * creation: the fetch only runs once (subsequent calls after e.g. a rotation are ignored while a
     * result is already present). Use [retry] to force a reload.
     */
    fun loadReferrers(period: StatsPeriod) {
        this.period = period
        if (hasStartedLoading) return
        load()
    }

    fun retry() = load()

    private fun load() {
        val period = period ?: return
        val site = selectedSiteRepository.getSelectedSite()
        val accessToken = accountStore.accessToken
        if (site == null || accessToken.isNullOrEmpty()) {
            _uiState.value = MostViewedDetailUiState.Error(resourceProvider.getString(R.string.stats_error_api))
            return
        }
        hasStartedLoading = true
        statsRepository.init(accessToken)
        _uiState.value = MostViewedDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = fetchReferrers(site.siteId, period)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchReferrers(siteId: Long, period: StatsPeriod): MostViewedDetailUiState =
        try {
            val result = statsRepository.fetchReferrersDetail(siteId = siteId, period = period)
            when (result) {
                is MostViewedResult.Success -> {
                    val items = result.items.map { it.toDetailItem() }
                    MostViewedDetailUiState.Loaded(
                        items = items,
                        maxViewsForBar = items.firstOrNull()?.views ?: 1L,
                        totalViews = result.totalViews,
                        totalViewsChange = result.totalViewsChange,
                        totalViewsChangePercent = result.totalViewsChangePercent,
                        dateRange = period.toDateRangeString(resourceProvider)
                    )
                }
                is MostViewedResult.Error -> MostViewedDetailUiState.Error(
                    resourceProvider.getString(R.string.stats_error_api)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(AppLog.T.STATS, "Error fetching referrers detail: ${e.message}", e)
            MostViewedDetailUiState.Error(resourceProvider.getString(R.string.stats_error_unknown))
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
