package org.wordpress.android.ui.newstats.mostpopulartime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.StatsInsightsData
import org.wordpress.android.ui.newstats.repository.InsightsResult
import org.wordpress.android.ui.newstats.repository.StatsInsightsUseCase
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MostPopularTimeViewModel @Inject constructor(
    private val selectedSiteRepository:
        SelectedSiteRepository,
    private val resourceProvider: ResourceProvider,
    private val statsInsightsUseCase: StatsInsightsUseCase
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<MostPopularTimeCardUiState>(
            MostPopularTimeCardUiState.Loading
        )
    val uiState: StateFlow<MostPopularTimeCardUiState> =
        _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> =
        _isRefreshing.asStateFlow()

    private var isLoading = false

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
                MostPopularTimeCardUiState.Error(
                    message = resourceProvider.getString(
                        R.string.stats_error_no_site
                    )
                )
            return
        }

        _uiState.value = MostPopularTimeCardUiState.Loading

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
            val result = statsInsightsUseCase(
                siteId,
                forceRefresh
            )
            when (result) {
                is InsightsResult.Success -> {
                    isLoadedSuccessfully = true
                    _uiState.value = mapToUiState(
                        result.data
                    )
                }
                is InsightsResult.Error -> {
                    isLoadedSuccessfully = false
                    _uiState.value =
                        MostPopularTimeCardUiState.Error(
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
                "Error loading most popular time:" +
                    " ${e.message}",
                e
            )
            _uiState.value =
                MostPopularTimeCardUiState.Error(
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
        internal fun mapToUiState(
            data: StatsInsightsData
        ): MostPopularTimeCardUiState {
            if (data.highestDayPercent == 0.0 ||
                data.highestHourPercent == 0.0
            ) {
                return MostPopularTimeCardUiState.NoData
            }
            return MostPopularTimeCardUiState.Loaded(
                bestDay = formatDayOfWeek(
                    data.highestDayOfWeek
                ),
                bestDayPercent = formatPercent(
                    data.highestDayPercent
                ),
                bestHour = formatHour(
                    data.highestHour
                ),
                bestHourPercent = formatPercent(
                    data.highestHourPercent
                )
            )
        }

        private const val DAYS_IN_WEEK = 7

        private fun formatDayOfWeek(
            wpDayOfWeek: Int
        ): String {
            // WordPress API: 0=Monday, 6=Sunday
            if (wpDayOfWeek !in 0 until DAYS_IN_WEEK) {
                return ""
            }
            val dayOfWeek =
                DayOfWeek.of((wpDayOfWeek % DAYS_IN_WEEK) + 1)
            return dayOfWeek.getDisplayName(
                TextStyle.FULL,
                Locale.getDefault()
            )
        }

        private fun formatHour(hour: Int): String {
            val time = LocalTime.of(hour, 0)
            return time.format(
                DateTimeFormatter.ofLocalizedTime(
                    FormatStyle.SHORT
                )
            )
        }

        private fun formatPercent(
            percent: Double
        ): String {
            return kotlin.math.round(percent)
                .toInt().toString()
        }
    }
}
