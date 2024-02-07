package org.wordpress.android.ui.sitemonitor

import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SiteMonitorParentViewModel @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val siteMonitorUtils: SiteMonitorUtils,
    private val metricsViewModel: SiteMonitorTabViewModelSlice,
    private val phpLogViewModel: SiteMonitorTabViewModelSlice,
    private val webServerViewModel: SiteMonitorTabViewModelSlice
) : ScopedViewModel(bgDispatcher) {
    private lateinit var site: SiteModel

    init {
        metricsViewModel.initialize(viewModelScope)
        phpLogViewModel.initialize(viewModelScope)
        webServerViewModel.initialize(viewModelScope)
    }

    fun start(site: SiteModel) {
        this.site = site
        siteMonitorUtils.trackActivityLaunched()
        loadData()
    }

    fun loadData() {
        metricsViewModel.start(SiteMonitorType.METRICS, SiteMonitorTabItem.Metrics.urlTemplate, site)
        phpLogViewModel.start(SiteMonitorType.PHP_LOGS, SiteMonitorTabItem.PHPLogs.urlTemplate, site)
        webServerViewModel.start(SiteMonitorType.WEB_SERVER_LOGS, SiteMonitorTabItem.WebServerLogs.urlTemplate, site)
    }

    fun getUiState(siteMonitorType: SiteMonitorType): State<SiteMonitorUiState> {
        return when (siteMonitorType) {
            SiteMonitorType.METRICS -> {
                metricsViewModel.uiState
            }

            SiteMonitorType.PHP_LOGS -> {
                phpLogViewModel.uiState
            }

            SiteMonitorType.WEB_SERVER_LOGS -> {
                webServerViewModel.uiState
            }
        }
    }

    fun onUrlLoaded(siteMonitorType: SiteMonitorType) {
        when (siteMonitorType) {
            SiteMonitorType.METRICS -> {
                metricsViewModel.onUrlLoaded()
            }

            SiteMonitorType.PHP_LOGS -> {
                phpLogViewModel.onUrlLoaded()
            }

            SiteMonitorType.WEB_SERVER_LOGS -> {
                webServerViewModel.onUrlLoaded()
            }
        }
    }

    fun onWebViewError(siteMonitorType: SiteMonitorType) {
        when (siteMonitorType) {
            SiteMonitorType.METRICS -> {
                metricsViewModel.onWebViewError()
            }

            SiteMonitorType.PHP_LOGS -> {
                phpLogViewModel.onWebViewError()
            }

            SiteMonitorType.WEB_SERVER_LOGS -> {
                webServerViewModel.onWebViewError()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        metricsViewModel.onCleared()
        phpLogViewModel.onCleared()
        webServerViewModel.onCleared()
    }

    fun getRefreshState(siteMonitorType: SiteMonitorType): State<Boolean> {
        return when (siteMonitorType) {
            SiteMonitorType.METRICS -> {
                metricsViewModel.isRefreshing
            }

            SiteMonitorType.PHP_LOGS -> {
                phpLogViewModel.isRefreshing
            }

            SiteMonitorType.WEB_SERVER_LOGS -> {
                webServerViewModel.isRefreshing
            }
        }
    }

    fun refreshData(siteMonitorType: SiteMonitorType) {
        when(siteMonitorType) {
            SiteMonitorType.METRICS -> {
                metricsViewModel.refreshData()
            }

            SiteMonitorType.PHP_LOGS -> {
                phpLogViewModel.refreshData()
            }

            SiteMonitorType.WEB_SERVER_LOGS -> {
                webServerViewModel.refreshData()
            }
        }
    }
}
