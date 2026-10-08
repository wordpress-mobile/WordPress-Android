package org.wordpress.android.ui.newstats.mostviewed

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.StatsCacheBucket
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

private const val CARD_MAX_ITEMS = 10

/**
 * Abstract base class for stats card ViewModels that share
 * the same loading/refresh/period/error handling pattern.
 *
 * Subclasses only need to provide:
 * - [logTag]: a label for error logging
 * - [cacheBucket]: the repository cache the card reads, so a
 *   period it already holds renders without a placeholder
 * - [fetchStats]: the suspend function that calls the
 *   repository and maps the result to [StatsCardFetchResult]
 */
abstract class BaseStatsCardViewModel(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    protected val statsRepository: StatsRepository,
    protected val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<MostViewedCardUiState>(
        MostViewedCardUiState.Loading
    )
    val uiState: StateFlow<MostViewedCardUiState> =
        _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> =
        _isRefreshing.asStateFlow()

    private var currentPeriod: StatsPeriod = StatsPeriod.Last7Days
    private var loadingPeriod: StatsPeriod? = null
    private var loadedPeriod: StatsPeriod? = null
    private var fetchJob: Job? = null
    protected abstract val logTag: String
    protected abstract val cacheBucket: StatsCacheBucket

    protected abstract suspend fun fetchStats(
        siteId: Long,
        period: StatsPeriod,
        forceRefresh: Boolean
    ): StatsCardFetchResult

    fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            loadingPeriod = null
            _uiState.value = MostViewedCardUiState.Error(
                resourceProvider.getString(
                    R.string.stats_error_no_site
                )
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            loadingPeriod = null
            _uiState.value = MostViewedCardUiState.Error(
                resourceProvider.getString(
                    R.string.stats_error_api
                )
            )
            return
        }

        val period = currentPeriod
        // A period already in memory repaints in the same frame, so showing the placeholder first
        // would only flash a skeleton over data the card is about to render anyway.
        if (!statsRepository.isCached(cacheBucket, site.siteId, period)) {
            _uiState.value = MostViewedCardUiState.Loading
        }

        launchFetch {
            try {
                fetchAndProcess(site)
                revalidateIfNeeded(site, period)
            } finally {
                clearLoadingPeriodIfCurrent()
            }
        }
    }

    /**
     * Replaces any in-flight fetch with [block]. A background revalidation outlives the load that
     * started it, so without this the period the user just left could still write its result into
     * the card after the new period has rendered.
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
     */
    private suspend fun clearLoadingPeriodIfCurrent() {
        if (fetchJob === coroutineContext.job) {
            loadingPeriod = null
        }
    }

    /**
     * Refreshes a result that was served from a previous visit to the stats screen, once. The card
     * already shows those numbers, so this runs without a loading state and, if it fails, leaves
     * them in place rather than replacing them with an error.
     */
    private suspend fun revalidateIfNeeded(site: SiteModel, period: StatsPeriod) {
        if (period != currentPeriod) return
        if (!statsRepository.needsRevalidation(cacheBucket, site.siteId, period)) return
        fetchAndProcess(site, forceRefresh = true, applyErrors = false)
    }

    fun refresh() {
        val site =
            selectedSiteRepository.getSelectedSite() ?: return
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) return

        loadingPeriod = currentPeriod
        launchFetch {
            try {
                _isRefreshing.value = true
                fetchAndProcess(site, forceRefresh = true)
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
        if (loadedPeriod == period || loadingPeriod == period) {
            return
        }
        loadingPeriod = period
        currentPeriod = period
        loadData()
    }

    /**
     * The period currently selected for this card. The detail screen self-fetches its own (unbounded)
     * data for this period rather than receiving the full list via the Intent.
     */
    fun getCurrentPeriod(): StatsPeriod = currentPeriod

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchAndProcess(
        site: SiteModel,
        forceRefresh: Boolean = false,
        applyErrors: Boolean = true
    ) {
        val siteId = site.siteId

        try {
            when (
                val result = fetchStats(
                    siteId, currentPeriod, forceRefresh
                )
            ) {
                is StatsCardFetchResult.Success -> {
                    loadedPeriod = currentPeriod

                    if (result.items.isEmpty()) {
                        _uiState.value =
                            MostViewedCardUiState.Loaded(
                                items = emptyList(),
                                maxViewsForBar = 0
                            )
                    } else {
                        val cardItems = result.items
                            .take(CARD_MAX_ITEMS)
                        val maxForBar =
                            cardItems.firstOrNull()?.views ?: 0L

                        _uiState.value =
                            MostViewedCardUiState.Loaded(
                                items = cardItems.map {
                                    it.toMostViewedItem()
                                },
                                maxViewsForBar = maxForBar
                            )
                    }
                }
                is StatsCardFetchResult.Error -> {
                    if (!applyErrors) return
                    _uiState.value = MostViewedCardUiState.Error(
                        message = resourceProvider.getString(
                            result.messageResId
                        ),
                        isAuthError = result.isAuthError,
                        isNotAvailable = result.isNotAvailable
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.STATS,
                "Error fetching $logTag",
                e
            )
            if (!applyErrors) return
            _uiState.value = MostViewedCardUiState.Error(
                resourceProvider.getString(
                    R.string.stats_error_unknown
                )
            )
        }
    }

    private fun MostViewedDetailItem.toMostViewedItem() =
        MostViewedItem(
            id = id,
            title = title,
            views = views,
            change = change,
            children = children,
            url = url
        )
}

sealed class StatsCardFetchResult {
    data class Success(
        val items: List<MostViewedDetailItem>,
        val totalValue: Long,
        val totalValueChange: Long,
        val totalValueChangePercent: Double
    ) : StatsCardFetchResult()

    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false,
        val isNotAvailable: Boolean = false
    ) : StatsCardFetchResult()
}
