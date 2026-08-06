package org.wordpress.android.ui.newstats.latestpost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.LatestPostResult
import org.wordpress.android.ui.newstats.repository.StatsLatestPostUseCase
import org.wordpress.android.ui.newstats.util.formatStatsDateTime
import org.wordpress.android.util.AppLog
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

@HiltViewModel
class LatestPostViewModel @Inject constructor(
    private val selectedSiteRepository:
        SelectedSiteRepository,
    private val statsLatestPostUseCase:
        StatsLatestPostUseCase
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<LatestPostCardUiState>(
            LatestPostCardUiState.Loading
        )
    val uiState: StateFlow<LatestPostCardUiState> =
        _uiState.asStateFlow()

    private val isLoaded = AtomicBoolean(false)
    private val isLoading = AtomicBoolean(false)
    // Main-thread-confined: only accessed from
    // viewModelScope (Dispatchers.Main).
    private var fetchJob: Job? = null

    fun loadData() {
        if (isLoaded.get() ||
            !isLoading.compareAndSet(false, true)
        ) return
        fetchData()
    }

    fun refresh() {
        fetchJob?.cancel()
        isLoaded.set(false)
        isLoading.set(true)
        _uiState.value = LatestPostCardUiState.Loading
        fetchData()
    }

    @Suppress(
        "TooGenericExceptionCaught",
        "InstanceOfCheckForException"
    )
    private fun fetchData() {
        val site = selectedSiteRepository
            .getSelectedSite()
        if (site == null) {
            isLoading.set(false)
            _uiState.value = LatestPostCardUiState.Error
            return
        }

        fetchJob = viewModelScope.launch {
            try {
                val result =
                    statsLatestPostUseCase(site)
                isLoaded.set(
                    result !is LatestPostResult.Error
                )
                handleResult(result)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLog.e(
                    AppLog.T.STATS,
                    "Error fetching latest post: " +
                        "${e.message}",
                    e
                )
                isLoaded.set(false)
                _uiState.value =
                    LatestPostCardUiState.Error
            } finally {
                isLoading.set(false)
            }
        }
    }

    private fun handleResult(result: LatestPostResult) {
        _uiState.value = when (result) {
            is LatestPostResult.Success -> {
                val views = result.data
                LatestPostCardUiState.Loaded(
                    postId = views.postId,
                    postTitle = views.postTitle,
                    postDate = formatStatsDateTime(
                        views.postDate
                    ),
                    views = views.totalViews,
                    likes = views.likeCount,
                    comments = views.commentCount,
                    recentViews = views.recentDailyViews
                )
            }
            is LatestPostResult.NoPosts ->
                LatestPostCardUiState.NoData
            is LatestPostResult.Error ->
                LatestPostCardUiState.Error
        }
    }
}
