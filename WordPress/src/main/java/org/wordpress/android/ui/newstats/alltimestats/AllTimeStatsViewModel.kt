package org.wordpress.android.ui.newstats.alltimestats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsSummaryResult
import org.wordpress.android.ui.newstats.repository.StatsSummaryUseCase
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class AllTimeStatsViewModel @Inject constructor(
    private val selectedSiteRepository:
        SelectedSiteRepository,
    private val resourceProvider: ResourceProvider,
    private val statsSummaryUseCase: StatsSummaryUseCase
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<AllTimeStatsCardUiState>(
            AllTimeStatsCardUiState.Loading
        )
    val uiState: StateFlow<AllTimeStatsCardUiState> =
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
            _uiState.value = AllTimeStatsCardUiState.Error(
                message = resourceProvider.getString(
                    R.string.stats_error_no_site
                )
            )
            return
        }

        _uiState.value = AllTimeStatsCardUiState.Loading

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
                    _uiState.value =
                        AllTimeStatsCardUiState.Loaded(
                            views = result.data.views,
                            visitors =
                                result.data.visitors,
                            posts = result.data.posts,
                            comments =
                                result.data.comments
                        )
                }
                is StatsSummaryResult.Error -> {
                    isLoadedSuccessfully = false
                    _uiState.value =
                        AllTimeStatsCardUiState.Error(
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
                "Error loading stats summary: " +
                    "${e.message}",
                e
            )
            _uiState.value =
                AllTimeStatsCardUiState.Error(
                    message = resourceProvider.getString(
                        R.string.stats_error_unknown
                    )
                )
        }
    }

    fun onRetry() {
        loadData()
    }
}
