package org.wordpress.android.ui.newstats.mostpopularday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.StatsSummaryData
import org.wordpress.android.ui.newstats.repository.StatsSummaryResult
import org.wordpress.android.ui.newstats.repository.StatsSummaryUseCase
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MostPopularDayViewModel @Inject constructor(
    private val selectedSiteRepository:
        SelectedSiteRepository,
    private val resourceProvider: ResourceProvider,
    private val statsSummaryUseCase: StatsSummaryUseCase
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<MostPopularDayCardUiState>(
            MostPopularDayCardUiState.Loading
        )
    val uiState: StateFlow<MostPopularDayCardUiState> =
        _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> =
        _isRefreshing.asStateFlow()

    @Volatile
    private var isLoading = false

    @Volatile
    private var isLoadedSuccessfully = false

    fun loadDataIfNeeded() {
        if (isLoadedSuccessfully || isLoading) return
        isLoading = true
        loadData()
    }

    fun refresh() {
        val site = selectedSiteRepository
            .getSelectedSite() ?: return
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                loadDataInternal(
                    site.siteId,
                    forceRefresh = true
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            isLoading = false
            _uiState.value =
                MostPopularDayCardUiState.Error(
                    message = resourceProvider.getString(
                        R.string.stats_error_no_site
                    )
                )
            return
        }

        _uiState.value = MostPopularDayCardUiState.Loading

        viewModelScope.launch {
            try {
                loadDataInternal(site.siteId)
            } finally {
                isLoading = false
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadDataInternal(
        siteId: Long,
        forceRefresh: Boolean = false
    ) {
        try {
            val result = statsSummaryUseCase(
                siteId,
                forceRefresh
            )
            when (result) {
                is StatsSummaryResult.Success -> {
                    isLoadedSuccessfully = true
                    _uiState.value = mapToUiState(
                        result.data
                    )
                }
                is StatsSummaryResult.Error -> {
                    isLoadedSuccessfully = false
                    _uiState.value =
                        MostPopularDayCardUiState.Error(
                            message = resourceProvider
                                .getString(
                                    R.string
                                        .stats_error_api
                                )
                        )
                }
            }
        } catch (e: Exception) {
            isLoadedSuccessfully = false
            AppLog.e(
                AppLog.T.STATS,
                "Error loading most popular day: " +
                    "${e.message}",
                e
            )
            _uiState.value =
                MostPopularDayCardUiState.Error(
                    message = resourceProvider.getString(
                        R.string.stats_error_unknown
                    )
                )
        }
    }

    fun onRetry() {
        loadData()
    }

    companion object {
        private val INPUT_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE
        private const val DISPLAY_PATTERN = "MMMM d"
        private const val PERCENTAGE_MULTIPLIER = 100.0

        internal fun mapToUiState(
            data: StatsSummaryData
        ): MostPopularDayCardUiState {
            val bestDay = data.viewsBestDay
            if (bestDay.isBlank()) {
                return MostPopularDayCardUiState.NoData
            }
            val parsed = parseBestDay(bestDay)
            val totalViews = data.views
            val bestDayViews = data.viewsBestDayTotal
            val percentage = if (totalViews > 0) {
                val pct = bestDayViews.toDouble() /
                    totalViews.toDouble() *
                    PERCENTAGE_MULTIPLIER
                String.format(
                    Locale.getDefault(),
                    "%.1f",
                    pct
                )
            } else {
                "0"
            }
            return MostPopularDayCardUiState.Loaded(
                dayAndMonth = parsed.first,
                year = parsed.second,
                views = bestDayViews,
                viewsPercentage = percentage
            )
        }

        private fun parseBestDay(
            bestDay: String
        ): Pair<String, String> {
            return try {
                val date = LocalDate.parse(
                    bestDay,
                    INPUT_FORMAT
                )
                val displayFormat =
                    DateTimeFormatter.ofPattern(
                        DISPLAY_PATTERN,
                        Locale.getDefault()
                    )
                val dayMonth = date.format(displayFormat)
                    .replaceFirstChar { it.uppercase() }
                val year = date.year.toString()
                dayMonth to year
            } catch (
                @Suppress(
                    "SwallowedException",
                    "TooGenericExceptionCaught"
                )
                e: Exception
            ) {
                bestDay to ""
            }
        }
    }
}
