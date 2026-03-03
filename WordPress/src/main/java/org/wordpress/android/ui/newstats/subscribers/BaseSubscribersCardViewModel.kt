package org.wordpress.android.ui.newstats.subscribers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

@Suppress("TooGenericExceptionCaught")
abstract class BaseSubscribersCardViewModel<UiState : Any>(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    protected val statsRepository: StatsRepository,
    protected val resourceProvider: ResourceProvider,
    initialState: UiState
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> =
        _isRefreshing.asStateFlow()

    private val isLoading = AtomicBoolean(false)
    private val isLoadedSuccessfully = AtomicBoolean(false)
    private var loadJob: Job? = null

    protected abstract val loadingState: UiState
    protected abstract fun errorState(
        message: String,
        isAuthError: Boolean = false
    ): UiState
    protected abstract suspend fun loadDataInternal(
        siteId: Long
    )

    fun loadDataIfNeeded() {
        if (isLoadedSuccessfully.get() ||
            !isLoading.compareAndSet(false, true)
        ) return
        loadData()
    }

    fun refresh() {
        val site = selectedSiteRepository.getSelectedSite()
            ?: return
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) return
        statsRepository.init(accessToken)
        resetLoadedSuccessfully()
        isLoading.set(true)
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                _isRefreshing.value = true
                fetchData(site.siteId)
            } finally {
                _isRefreshing.value = false
                isLoading.set(false)
            }
        }
    }

    fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            isLoading.set(false)
            updateState(
                errorState(
                    resourceProvider.getString(
                        R.string.stats_error_no_site
                    )
                )
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            isLoading.set(false)
            updateState(
                errorState(
                    resourceProvider.getString(
                        R.string.stats_error_not_authenticated
                    )
                )
            )
            return
        }

        statsRepository.init(accessToken)
        updateState(loadingState)

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                fetchData(site.siteId)
            } finally {
                isLoading.set(false)
            }
        }
    }

    private suspend fun fetchData(siteId: Long) {
        try {
            loadDataInternal(siteId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.STATS,
                "Error loading stats data",
                e
            )
            updateState(
                errorState(
                    resourceProvider.getString(
                        R.string.stats_error_unknown
                    )
                )
            )
        }
    }

    protected fun getSiteId(): Long? =
        selectedSiteRepository.getSelectedSite()?.siteId

    protected fun updateState(state: UiState) {
        _uiState.value = state
    }

    protected fun markLoadedSuccessfully() {
        isLoadedSuccessfully.set(true)
    }

    protected fun resetLoadedSuccessfully() {
        isLoadedSuccessfully.set(false)
    }
}
