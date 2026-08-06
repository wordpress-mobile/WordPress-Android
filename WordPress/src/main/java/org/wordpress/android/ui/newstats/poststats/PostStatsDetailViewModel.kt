package org.wordpress.android.ui.newstats.poststats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.PostViewsData
import org.wordpress.android.ui.newstats.datasource.PostViewsDailyView
import org.wordpress.android.ui.newstats.repository.PostViewsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

@HiltViewModel
class PostStatsDetailViewModel @Inject constructor(
    private val selectedSiteRepository:
        SelectedSiteRepository,
    private val statsRepository: StatsRepository,
    private val accountStore: AccountStore,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<PostStatsDetailUiState>(
            PostStatsDetailUiState.Loading
        )
    val uiState: StateFlow<PostStatsDetailUiState> =
        _uiState.asStateFlow()

    @Suppress(
        "TooGenericExceptionCaught",
        "InstanceOfCheckForException"
    )
    fun loadData(postId: Long) {
        val site = selectedSiteRepository.getSelectedSite()
        val token = accountStore.accessToken
        if (site == null || token.isNullOrEmpty()) {
            _uiState.value = PostStatsDetailUiState.Error(
                resourceProvider.getString(
                    R.string.stats_error_no_site
                )
            )
            return
        }
        statsRepository.init(token)

        _uiState.value = PostStatsDetailUiState.Loading
        viewModelScope.launch {
            try {
                val result =
                    statsRepository.fetchPostViews(
                        siteId = site.siteId,
                        postId = postId
                    )
                _uiState.value = when (result) {
                    is PostViewsResult.Success ->
                        PostStatsDetailUiState.Loaded(
                            data = result.data,
                            // The newest day is selected first, as the old stats screen does.
                            selectedDayIndex = result.data
                                .dailyViews.lastIndex
                        )
                    is PostViewsResult.Error ->
                        PostStatsDetailUiState.Error(
                            resourceProvider.getString(
                                R.string.stats_error_api
                            )
                        )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLog.e(
                    AppLog.T.STATS,
                    "Error fetching post views: " +
                        "${e.message}",
                    e
                )
                _uiState.value =
                    PostStatsDetailUiState.Error(
                        resourceProvider.getString(
                            R.string.stats_error_unknown
                        )
                    )
            }
        }
    }

    /** Moves the selected day one step towards the start of the history. */
    fun selectPreviousDay() = shiftSelectedDay(-1)

    /** Moves the selected day one step towards the present. */
    fun selectNextDay() = shiftSelectedDay(1)

    private fun shiftSelectedDay(offset: Int) {
        _uiState.update { state ->
            if (state !is PostStatsDetailUiState.Loaded) {
                return@update state
            }
            val target = state.selectedDayIndex + offset
            if (target in state.data.dailyViews.indices) {
                state.copy(selectedDayIndex = target)
            } else {
                state
            }
        }
    }
}

sealed class PostStatsDetailUiState {
    data object Loading : PostStatsDetailUiState()

    data class Loaded(
        val data: PostViewsData,
        /** Index into [PostViewsData.dailyViews]; -1 when the post has no history. */
        val selectedDayIndex: Int
    ) : PostStatsDetailUiState() {
        val selectedDay: PostViewsDailyView?
            get() = data.dailyViews
                .getOrNull(selectedDayIndex)

        /** The day before the selected one, which the change is measured against. */
        val previousDay: PostViewsDailyView?
            get() = data.dailyViews
                .getOrNull(selectedDayIndex - 1)

        val hasPreviousDay: Boolean
            get() = selectedDayIndex > 0

        val hasNextDay: Boolean
            get() = selectedDayIndex <
                data.dailyViews.lastIndex

        /**
         * The window of days the chart draws, ending at the selected day so it is always the
         * rightmost bar -- the same framing the old stats screen uses.
         */
        val chartDays: List<PostViewsDailyView>
            get() = data.dailyViews
                .take(selectedDayIndex + 1)
                .takeLast(CHART_DAYS)
    }

    data class Error(
        val message: String
    ) : PostStatsDetailUiState()

    companion object {
        // Two weeks of bars, matching the old stats post detail chart.
        const val CHART_DAYS = 14
    }
}
