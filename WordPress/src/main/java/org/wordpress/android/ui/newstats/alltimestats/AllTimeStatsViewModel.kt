package org.wordpress.android.ui.newstats.alltimestats

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.repository.StatsSummaryResult
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class AllTimeStatsViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<AllTimeStatsCardUiState>(
            AllTimeStatsCardUiState.Loading
        )
    val uiState: StateFlow<AllTimeStatsCardUiState> =
        _uiState.asStateFlow()

    fun handleResult(result: StatsSummaryResult) {
        _uiState.value = when (result) {
            is StatsSummaryResult.Success ->
                AllTimeStatsCardUiState.Loaded(
                    views = result.data.views,
                    visitors = result.data.visitors,
                    posts = result.data.posts,
                    comments = result.data.comments
                )
            is StatsSummaryResult.Error ->
                AllTimeStatsCardUiState.Error(
                    message = resourceProvider
                        .getString(
                            R.string.stats_error_api
                        )
                )
        }
    }

    fun showLoading() {
        _uiState.value = AllTimeStatsCardUiState.Loading
    }
}
