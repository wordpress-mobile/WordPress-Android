package org.wordpress.android.ui.newstats.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.DevicesResult
import org.wordpress.android.ui.newstats.repository.StatsCacheBucket
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.util.statsUpgradeUrl
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val CARD_MAX_ITEMS = 10

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository
) : ViewModel() {
    private val _screensizeUiState =
        MutableStateFlow<DevicesCardUiState>(DevicesCardUiState.Loading)
    private val _browserUiState =
        MutableStateFlow<DevicesCardUiState>(DevicesCardUiState.Loading)
    private val _platformUiState =
        MutableStateFlow<DevicesCardUiState>(DevicesCardUiState.Loading)

    private val _selectedDeviceType =
        MutableStateFlow(DeviceType.SCREENSIZE)
    val selectedDeviceType: StateFlow<DeviceType> =
        _selectedDeviceType.asStateFlow()

    val uiState: StateFlow<DevicesCardUiState> = combine(
        _selectedDeviceType,
        _screensizeUiState,
        _browserUiState,
        _platformUiState
    ) { type, screensize, browser, platform ->
        when (type) {
            DeviceType.SCREENSIZE -> screensize
            DeviceType.BROWSER -> browser
            DeviceType.PLATFORM -> platform
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        DevicesCardUiState.Loading
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentPeriod: StatsPeriod = StatsPeriod.Last7Days

    // Per-type loaded period tracking for lazy fetching
    private var screensizeLoadedPeriod: StatsPeriod? = null
    private var browserLoadedPeriod: StatsPeriod? = null
    private var platformLoadedPeriod: StatsPeriod? = null

    private val fetchJobs = mutableMapOf<DeviceType, Job>()

    fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            setAllStatesError(R.string.stats_error_no_site)
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            setAllStatesError(R.string.stats_error_api)
            return
        }

        val period = currentPeriod
        val type = _selectedDeviceType.value
        // A period already in memory repaints in the same frame, so the placeholder would only
        // flash a skeleton over data the card is about to render anyway.
        if (!isCached(type, site.siteId, period)) {
            setCurrentTypeLoading()
        }

        launchFetch(type) {
            fetchForCurrentType(site)
            revalidateIfNeeded(type, site, period)
        }
    }

    /**
     * Replaces any in-flight fetch for [type]. A background revalidation outlives the load that
     * started it, so without this the period the user just left could still write its result into
     * the card after the new period has rendered.
     */
    private fun launchFetch(type: DeviceType, block: suspend () -> Unit) {
        fetchJobs[type]?.cancel()
        fetchJobs[type] = viewModelScope.launch { block() }
    }

    private fun cancelAllFetchJobs() {
        fetchJobs.values.forEach { it.cancel() }
        fetchJobs.clear()
    }

    private fun isCached(type: DeviceType, siteId: Long, period: StatsPeriod) =
        statsRepository.isCached(bucketFor(type), siteId, period)

    /**
     * Refreshes a result served from a previous visit to the stats screen, once. The card already
     * shows those numbers, so this runs without a loading state and keeps them if it fails.
     */
    private suspend fun revalidateIfNeeded(
        type: DeviceType,
        site: SiteModel,
        period: StatsPeriod
    ) {
        if (period != currentPeriod) return
        if (!statsRepository.needsRevalidation(bucketFor(type), site.siteId, period)) return
        fetchForType(type, site, forceRefresh = true, applyErrors = false)
    }

    private fun bucketFor(type: DeviceType) = when (type) {
        DeviceType.SCREENSIZE -> StatsCacheBucket.DEVICES_SCREENSIZE
        DeviceType.BROWSER -> StatsCacheBucket.DEVICES_BROWSER
        DeviceType.PLATFORM -> StatsCacheBucket.DEVICES_PLATFORM
    }

    fun refresh() {
        val site = selectedSiteRepository.getSelectedSite() ?: return
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) return

        launchFetch(_selectedDeviceType.value) {
            try {
                _isRefreshing.value = true
                resetLoadedPeriodForCurrentType()
                fetchForCurrentType(site, forceRefresh = true)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun onRetry() {
        loadData()
    }

    fun getAdminUrl(): String? =
        selectedSiteRepository.getSelectedSite()?.adminUrl

    fun getUpgradeUrl(): String? =
        selectedSiteRepository.getSelectedSite()?.statsUpgradeUrl()

    fun onPeriodChanged(period: StatsPeriod) {
        if (period == currentPeriod &&
            isTypeLoadedForCurrentPeriod(
                _selectedDeviceType.value
            )
        ) return
        // Every type's data is now for the wrong period, including any type the user is not
        // currently looking at.
        cancelAllFetchJobs()
        currentPeriod = period
        screensizeLoadedPeriod = null
        browserLoadedPeriod = null
        platformLoadedPeriod = null
        loadData()
    }

    @Suppress("ReturnCount")
    fun onDeviceTypeChanged(type: DeviceType) {
        if (_selectedDeviceType.value == type) return
        _selectedDeviceType.value = type

        if (!isTypeLoadedForCurrentPeriod(type)) {
            val site =
                selectedSiteRepository.getSelectedSite() ?: return
            val accessToken = accountStore.accessToken
            if (accessToken.isNullOrEmpty()) return

            val period = currentPeriod
            if (!isCached(type, site.siteId, period)) {
                setTypeLoading(type)
            }
            launchFetch(type) {
                fetchForType(type, site)
                revalidateIfNeeded(type, site, period)
            }
        }
    }

    private fun setAllStatesError(
        @StringRes messageResId: Int,
        isAuthError: Boolean = false
    ) {
        val error = DevicesCardUiState.Error(
            messageResId, isAuthError
        )
        _screensizeUiState.value = error
        _browserUiState.value = error
        _platformUiState.value = error
    }

    private fun setCurrentTypeLoading() {
        setTypeLoading(_selectedDeviceType.value)
    }

    private fun setTypeLoading(type: DeviceType) {
        when (type) {
            DeviceType.SCREENSIZE ->
                _screensizeUiState.value = DevicesCardUiState.Loading
            DeviceType.BROWSER ->
                _browserUiState.value = DevicesCardUiState.Loading
            DeviceType.PLATFORM ->
                _platformUiState.value = DevicesCardUiState.Loading
        }
    }

    private fun resetLoadedPeriodForCurrentType() {
        when (_selectedDeviceType.value) {
            DeviceType.SCREENSIZE ->
                screensizeLoadedPeriod = null
            DeviceType.BROWSER ->
                browserLoadedPeriod = null
            DeviceType.PLATFORM ->
                platformLoadedPeriod = null
        }
    }

    private fun isTypeLoadedForCurrentPeriod(
        type: DeviceType
    ): Boolean {
        return when (type) {
            DeviceType.SCREENSIZE ->
                screensizeLoadedPeriod == currentPeriod
            DeviceType.BROWSER ->
                browserLoadedPeriod == currentPeriod
            DeviceType.PLATFORM ->
                platformLoadedPeriod == currentPeriod
        }
    }

    private suspend fun fetchForCurrentType(site: SiteModel, forceRefresh: Boolean = false) {
        fetchForType(_selectedDeviceType.value, site, forceRefresh)
    }

    private suspend fun fetchForType(
        type: DeviceType,
        site: SiteModel,
        forceRefresh: Boolean = false,
        applyErrors: Boolean = true
    ) {
        val stateFlow = when (type) {
            DeviceType.SCREENSIZE -> _screensizeUiState
            DeviceType.BROWSER -> _browserUiState
            DeviceType.PLATFORM -> _platformUiState
        }
        val setLoadedPeriod: () -> Unit = when (type) {
            DeviceType.SCREENSIZE ->
                { -> screensizeLoadedPeriod = currentPeriod }
            DeviceType.BROWSER ->
                { -> browserLoadedPeriod = currentPeriod }
            DeviceType.PLATFORM ->
                { -> platformLoadedPeriod = currentPeriod }
        }
        fetchAndUpdate(stateFlow, setLoadedPeriod, applyErrors) {
            when (type) {
                DeviceType.SCREENSIZE ->
                    statsRepository.fetchDevicesScreensize(
                        site.siteId, currentPeriod, forceRefresh
                    )
                DeviceType.BROWSER ->
                    statsRepository.fetchDevicesBrowser(
                        site.siteId, currentPeriod, forceRefresh
                    )
                DeviceType.PLATFORM ->
                    statsRepository.fetchDevicesPlatform(
                        site.siteId, currentPeriod, forceRefresh
                    )
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchAndUpdate(
        stateFlow: MutableStateFlow<DevicesCardUiState>,
        setLoadedPeriod: () -> Unit,
        applyErrors: Boolean,
        fetch: suspend () -> DevicesResult
    ) {
        try {
            when (val result = fetch()) {
                is DevicesResult.Success -> {
                    setLoadedPeriod()
                    stateFlow.value = mapToLoadedState(result)
                }
                is DevicesResult.Error -> {
                    if (!applyErrors) return
                    stateFlow.value = DevicesCardUiState.Error(
                        result.messageResId,
                        result.isAuthError,
                        result.isPlanGated
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.STATS,
                "Error fetching devices data", e
            )
            if (!applyErrors) return
            stateFlow.value = DevicesCardUiState.Error(
                R.string.stats_error_unknown
            )
        }
    }

    private fun mapToLoadedState(
        result: DevicesResult.Success
    ): DevicesCardUiState {
        if (result.items.isEmpty()) {
            return DevicesCardUiState.Loaded(
                items = emptyList(),
                maxValueForBar = 0.0
            )
        }
        val cardItems = result.items
            .take(CARD_MAX_ITEMS)
            .map { DeviceItem(name = it.name, value = it.views) }
        val maxValueForBar =
            cardItems.maxOfOrNull { it.value } ?: 0.0
        return DevicesCardUiState.Loaded(
            items = cardItems,
            maxValueForBar = maxValueForBar
        )
    }
}
