package org.wordpress.android.ui.newstats.authors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.components.StatsViewChange
import org.wordpress.android.ui.newstats.repository.StatsCacheBucket
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.TopAuthorItemData
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.repository.TopAuthorsResult
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

private const val CARD_MAX_ITEMS = 10

@HiltViewModel
class AuthorsViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthorsCardUiState>(AuthorsCardUiState.Loading)
    val uiState: StateFlow<AuthorsCardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentPeriod: StatsPeriod = StatsPeriod.Last7Days
    private var loadingPeriod: StatsPeriod? = null
    private var loadedPeriod: StatsPeriod? = null
    private var fetchJob: Job? = null

    fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            loadingPeriod = null
            _uiState.value = AuthorsCardUiState.Error(
                R.string.stats_error_no_site
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            loadingPeriod = null
            _uiState.value = AuthorsCardUiState.Error(
                R.string.stats_error_api
            )
            return
        }

        val period = currentPeriod
        // A period already in memory repaints in the same frame, so the placeholder would only
        // flash a skeleton over data the card is about to render anyway.
        if (!statsRepository.isCached(StatsCacheBucket.AUTHORS, site.siteId, period)) {
            _uiState.value = AuthorsCardUiState.Loading
        }

        launchFetch {
            try {
                fetchTopAuthors(site)
                revalidateIfNeeded(site, period)
            } finally {
                clearLoadingPeriodIfCurrent()
            }
        }
    }

    /**
     * Replaces any in-flight fetch with [block]. A background revalidation outlives the load that
     * started it, so without this the period the user just left could still write into the card
     * after the new period has rendered.
     *
     * The job is registered before it is started, so [fetchJob] always holds the one in flight —
     * which is what [clearLoadingPeriodIfCurrent] compares against.
     */
    private fun launchFetch(block: suspend () -> Unit) {
        fetchJob?.cancel()
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) { block() }
        fetchJob = job
        job.start()
    }

    /**
     * Clears the guard that keeps a period from being requested twice, but only for the job that is
     * still current: a cancelled one clearing it would let the re-dispatch that follows a card being
     * added cancel and restart the load that is already fetching this period.
     *
     * Every fetch registered through [launchFetch] has to call this, including [refresh]: the job it
     * replaces skips the clear because it is no longer current, so whatever replaced it owns the
     * guard from then on.
     */
    private suspend fun clearLoadingPeriodIfCurrent() {
        if (fetchJob === coroutineContext.job) {
            loadingPeriod = null
        }
    }

    /**
     * Refreshes a result served from a previous visit to the stats screen, once. The card already
     * shows those numbers, so this runs without a loading state and keeps them if it fails.
     */
    private suspend fun revalidateIfNeeded(site: SiteModel, period: StatsPeriod) {
        if (period != currentPeriod) return
        if (!statsRepository.needsRevalidation(StatsCacheBucket.AUTHORS, site.siteId, period)) return
        fetchTopAuthors(site, forceRefresh = true, applyErrors = false)
    }

    fun refresh() {
        val site = selectedSiteRepository.getSelectedSite() ?: return
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) return

        loadingPeriod = currentPeriod
        launchFetch {
            try {
                _isRefreshing.value = true
                fetchTopAuthors(site, forceRefresh = true)
            } finally {
                // Not gated on the job: a cancelled refresh must still stop the spinner, because
                // whatever replaced it does not own it.
                _isRefreshing.value = false
                clearLoadingPeriodIfCurrent()
            }
        }
    }

    fun onRetry() {
        loadData()
    }

    fun getAdminUrl(): String? =
        selectedSiteRepository.getSelectedSite()?.adminUrl

    fun onPeriodChanged(period: StatsPeriod) {
        if (loadedPeriod == period || loadingPeriod == period) return
        loadingPeriod = period
        currentPeriod = period
        loadData()
    }

    /**
     * The period currently selected for the authors card. The authors detail screen self-fetches its
     * own (unbounded) data for this period rather than receiving the full list via the Intent.
     */
    fun getCurrentPeriod(): StatsPeriod = currentPeriod

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchTopAuthors(
        site: SiteModel,
        forceRefresh: Boolean = false,
        applyErrors: Boolean = true
    ) {
        val siteId = site.siteId

        try {
            when (val result = statsRepository.fetchTopAuthors(
                siteId, currentPeriod, forceRefresh
            )) {
                is TopAuthorsResult.Success -> {
                    loadedPeriod = currentPeriod

                    if (result.authors.isEmpty()) {
                        _uiState.value = AuthorsCardUiState.Loaded(
                            authors = emptyList(),
                            maxViewsForBar = 0,
                            hasMoreItems = false
                        )
                    } else {
                        val authors = result.authors.map { it.toAuthorUiItem() }

                        val cardAuthors = authors.take(CARD_MAX_ITEMS)
                        val maxViewsForBar =
                            cardAuthors.firstOrNull()?.views ?: 0L

                        _uiState.value = AuthorsCardUiState.Loaded(
                            authors = cardAuthors,
                            maxViewsForBar = maxViewsForBar,
                            hasMoreItems =
                                authors.size > CARD_MAX_ITEMS
                        )
                    }
                }
                is TopAuthorsResult.Error -> {
                    if (!applyErrors) return
                    _uiState.value = AuthorsCardUiState.Error(
                        result.messageResId,
                        result.isAuthError
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(AppLog.T.STATS, "Error fetching top authors", e)
            if (!applyErrors) return
            _uiState.value = AuthorsCardUiState.Error(
                R.string.stats_error_unknown
            )
        }
    }
}

/**
 * Maps a repository [TopAuthorItemData] to the parcelable [AuthorUiItem] shown by the card and the
 * detail screen. Shared by [AuthorsViewModel] and [AuthorsDetailViewModel].
 */
internal fun TopAuthorItemData.toAuthorUiItem() = AuthorUiItem(
    name = name,
    avatarUrl = avatarUrl,
    views = views,
    change = when {
        viewsChange > 0 -> StatsViewChange.Positive(viewsChange, abs(viewsChangePercent))
        viewsChange < 0 -> StatsViewChange.Negative(abs(viewsChange), abs(viewsChangePercent))
        else -> StatsViewChange.NoChange
    }
)
