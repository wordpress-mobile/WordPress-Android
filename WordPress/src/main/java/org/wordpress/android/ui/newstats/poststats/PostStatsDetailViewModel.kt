package org.wordpress.android.ui.newstats.poststats

import androidx.annotation.StringRes
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
import org.wordpress.android.ui.engagement.GetLikesUseCase
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState
import org.wordpress.android.ui.engagement.GetLikesUseCase.LikeGroupFingerPrint
import org.wordpress.android.ui.engagement.GetLikesUseCase.PaginationParams
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
    private val resourceProvider: ResourceProvider,
    private val getLikesUseCase: GetLikesUseCase
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
        if (postId == NO_POST_ID) {
            AppLog.w(
                AppLog.T.STATS,
                "Post stats opened without a post id"
            )
            showError(R.string.stats_error_unknown)
            return
        }
        val site = selectedSiteRepository.getSelectedSite()
        val token = accountStore.accessToken
        if (site == null || token.isNullOrEmpty()) {
            showError(R.string.stats_error_no_site)
            return
        }

        _uiState.value = PostStatsDetailUiState.Loading
        viewModelScope.launch {
            try {
                val result =
                    statsRepository.fetchPostViews(
                        siteId = site.siteId,
                        postId = postId
                    )
                when (result) {
                    is PostViewsResult.Success -> {
                        _uiState.value =
                            PostStatsDetailUiState.Loaded(
                                data = result.data,
                                // The newest day is selected first, as the old screen does.
                                selectedDayIndex = result.data
                                    .dailyViews.lastIndex
                            )
                        loadLikers(
                            siteId = site.siteId,
                            postId = postId,
                            likeCount = result.data.post
                                ?.likeCount ?: 0
                        )
                    }
                    is PostViewsResult.Error ->
                        showError(R.string.stats_error_api)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLog.e(
                    AppLog.T.STATS,
                    "Error fetching post views: " +
                        "${e.message}",
                    e
                )
                showError(R.string.stats_error_unknown)
            }
        }
    }

    /**
     * The faces for the header's likes strip, fetched only once the stats call has told us the
     * item has likes at all. It decorates a header that is already complete without it, so a
     * failure leaves the strip out rather than failing the screen: the like *count* comes from
     * the stats response and is shown either way.
     */
    private fun loadLikers(
        siteId: Long,
        postId: Long,
        likeCount: Long
    ) {
        if (likeCount <= 0) return
        viewModelScope.launch {
            getLikesUseCase.getLikesForPost(
                LikeGroupFingerPrint(
                    siteId,
                    postId,
                    likeCount.toInt()
                ),
                PaginationParams(
                    requestNextPage = false,
                    pageLength = LIKERS_PAGE_LENGTH
                )
            ).collect { state ->
                val likes = when (state) {
                    is GetLikesState.LikesData -> state.likes
                    // Whatever was cached is better than an empty strip, and is all a failed
                    // request leaves behind.
                    is GetLikesState.Failure -> state.cachedLikes
                    GetLikesState.Loading -> return@collect
                }
                val avatarUrls = likes.mapNotNull { like ->
                    like.likerAvatarUrl
                        ?.takeIf { it.isNotBlank() }
                }
                _uiState.update { current ->
                    if (current is PostStatsDetailUiState.Loaded) {
                        current.copy(
                            likerAvatarUrls = avatarUrls
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    override fun onCleared() {
        // GetLikesUseCase registers itself with the dispatcher on construction, so the instance
        // injected here has to be unregistered with the screen.
        getLikesUseCase.clear()
    }

    private fun showError(@StringRes messageResId: Int) {
        _uiState.value = PostStatsDetailUiState.Error(
            resourceProvider.getString(messageResId)
        )
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
            if (target in state.chartRange) {
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
        val selectedDayIndex: Int,
        /** Faces for the header's likes strip; empty until -- or unless -- they arrive. */
        val likerAvatarUrls: List<String> = emptyList()
    ) : PostStatsDetailUiState() {
        /**
         * The days the chart draws: a fixed trailing window, so stepping through days moves the
         * highlight within a stable set of bars rather than sliding the whole chart. This is the
         * framing the old stats screen uses, and it bounds how far back the arrows can go.
         */
        val chartDays: List<PostViewsDailyView> =
            data.dailyViews.subList(
                (data.dailyViews.size - CHART_DAYS)
                    .coerceAtLeast(0),
                data.dailyViews.size
            )

        /** Indices of [PostViewsData.dailyViews] the chart covers, i.e. what the arrows can reach. */
        val chartRange: IntRange =
            data.dailyViews.size - chartDays.size until
                data.dailyViews.size

        /** Position of the selected day within [chartDays]. */
        val selectedChartIndex: Int =
            selectedDayIndex - chartRange.first

        val selectedDay: PostViewsDailyView? =
            data.dailyViews.getOrNull(selectedDayIndex)

        /** The day before the selected one, which the change is measured against. */
        val previousDay: PostViewsDailyView? =
            data.dailyViews.getOrNull(selectedDayIndex - 1)

        /**
         * The arrows move within [chartRange], so they are bounded by the chart window rather than
         * by whether [previousDay]/the next day exist in the whole history.
         */
        val hasPreviousDay: Boolean =
            selectedDayIndex > chartRange.first

        val hasNextDay: Boolean =
            selectedDayIndex < chartRange.last
    }

    data class Error(
        val message: String
    ) : PostStatsDetailUiState()
}

/**
 * Deliberately the same page as GetLikesHandler's `LIKES_PER_PAGE_DEFAULT`, which is what the
 * likes list this screen opens asks for. The strip itself needs far fewer -- it draws five faces,
 * and its overflow count comes from the like count the stats response already carries -- but a
 * smaller page here is not worth what it costs.
 *
 * `OnPostLikesChanged` carries nothing saying which request it answers, and every `GetLikesUseCase`
 * registers itself on the dispatcher, so this screen's instance and the likes list's both resume on
 * whichever response for the post arrives first. Asking for the same page means it does not matter
 * which one that is: either answer is the answer both of them wanted. Ask for fewer and tapping
 * through to the list while this request is still in flight leaves it showing a short first page.
 *
 * So this is not a free number to tune: lower it and that crossed wire starts to show again.
 */
private const val LIKERS_PAGE_LENGTH = 20

// Two weeks of bars, matching the old stats post detail chart.
private const val CHART_DAYS = 14

/**
 * Stands in for a missing post id. Post 0 is the site's home page, so it can't double as "absent".
 */
internal const val NO_POST_ID = -1L
