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
                val post = views.post
                // The card always asks for a published post, so a missing post row means the
                // response wasn't what we asked for -- treat it as an error rather than
                // rendering a card with no title.
                if (post == null) {
                    AppLog.w(
                        AppLog.T.STATS,
                        "Latest post stats had no post row " +
                            "for id ${views.postId}"
                    )
                    LatestPostCardUiState.Error
                } else {
                    LatestPostCardUiState.Loaded(
                        postId = views.postId,
                        postTitle = post.title,
                        postDate = formatStatsDateTime(
                            post.date
                        ),
                        views = views.totalViews,
                        likes = post.likeCount,
                        comments = post.commentCount,
                        recentViews = views.dailyViews
                            .takeLast(CARD_CHART_DAYS)
                            .map { it.views },
                        featuredImageUrl =
                            result.featuredImageUrl
                    )
                }
            }
            is LatestPostResult.NoPosts ->
                LatestPostCardUiState.NoData
            is LatestPostResult.Error ->
                LatestPostCardUiState.Error
        }
    }

    companion object {
        // A week of daily views is all the card charts.
        private const val CARD_CHART_DAYS = 7
    }
}
