package org.wordpress.android.ui.newstats.authors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.TopAuthorsResult
import org.wordpress.android.ui.newstats.util.toDateRangeString
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

/**
 * Backs [AuthorsDetailActivity]. The authors card only shows the first N authors, so the detail
 * screen re-requests the full, unbounded list (max = 0) itself instead of receiving it through the
 * Intent, which could exceed the Binder transaction limit for sites with many authors.
 */
@HiltViewModel
class AuthorsDetailViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthorsDetailUiState>(AuthorsDetailUiState.Loading)
    val uiState: StateFlow<AuthorsDetailUiState> = _uiState.asStateFlow()

    private var period: StatsPeriod? = null
    private var hasStartedLoading = false

    /**
     * Loads the authors detail data for [period]. Safe to call on every [AuthorsDetailActivity]
     * creation: the fetch only runs once (subsequent calls after e.g. a rotation are ignored while a
     * result is already present). Use [retry] to force a reload.
     */
    fun load(period: StatsPeriod) {
        this.period = period
        if (hasStartedLoading) return
        fetch()
    }

    fun retry() = fetch()

    /**
     * The WP-Admin URL of the selected site, used to offer a re-authentication action when the
     * fetch fails with an auth error (mirrors the authors card and the Most Viewed detail screen).
     */
    fun getAdminUrl(): String? = selectedSiteRepository.getSelectedSite()?.adminUrl

    private fun fetch() {
        val period = period ?: return
        val site = selectedSiteRepository.getSelectedSite()
        val accessToken = accountStore.accessToken
        if (site == null || accessToken.isNullOrEmpty()) {
            _uiState.value = AuthorsDetailUiState.Error(resourceProvider.getString(R.string.stats_error_api))
            return
        }
        hasStartedLoading = true
        statsRepository.init(accessToken)
        _uiState.value = AuthorsDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = fetchAuthors(site.siteId, period)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchAuthors(siteId: Long, period: StatsPeriod): AuthorsDetailUiState =
        try {
            when (val result = statsRepository.fetchTopAuthors(siteId, period)) {
                is TopAuthorsResult.Success -> {
                    val authors = result.authors.map { it.toAuthorUiItem() }
                    AuthorsDetailUiState.Loaded(
                        authors = authors,
                        maxViewsForBar = authors.firstOrNull()?.views ?: 0L,
                        totalViews = result.totalViews,
                        totalViewsChange = result.totalViewsChange,
                        totalViewsChangePercent = result.totalViewsChangePercent,
                        dateRange = period.toDateRangeString(resourceProvider)
                    )
                }
                is TopAuthorsResult.Error -> AuthorsDetailUiState.Error(
                    message = resourceProvider.getString(result.messageResId),
                    isAuthError = result.isAuthError
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(AppLog.T.STATS, "Error fetching authors detail: ${e.message}", e)
            AuthorsDetailUiState.Error(resourceProvider.getString(R.string.stats_error_unknown))
        }
}

sealed interface AuthorsDetailUiState {
    data object Loading : AuthorsDetailUiState

    data class Loaded(
        val authors: List<AuthorUiItem>,
        val maxViewsForBar: Long,
        val totalViews: Long,
        val totalViewsChange: Long,
        val totalViewsChangePercent: Double,
        val dateRange: String
    ) : AuthorsDetailUiState

    data class Error(
        val message: String,
        val isAuthError: Boolean = false
    ) : AuthorsDetailUiState
}
