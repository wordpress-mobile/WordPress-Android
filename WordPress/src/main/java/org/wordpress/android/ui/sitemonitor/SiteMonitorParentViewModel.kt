package org.wordpress.android.ui.sitemonitor

import androidx.compose.runtime.MutableState
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SiteMonitorParentViewModel @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
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
        trackActivityLaunched()
        metricsViewModel.start(SiteMonitorType.METRICS, SiteMonitorTabItem.Metrics.urlTemplate, site)
        phpLogViewModel.start(SiteMonitorType.PHP_LOGS, SiteMonitorTabItem.PHPLogs.urlTemplate, site)
        webServerViewModel.start(SiteMonitorType.WEB_SERVER_LOGS, SiteMonitorTabItem.WebServerLogs.urlTemplate, site)
    }

    fun getUiState(siteMonitorType: SiteMonitorType): MutableState<SiteMonitorUiState> {
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

    fun onUrlLoaded(siteMonitorType: SiteMonitorType, url: String) {
        when (siteMonitorType) {
            SiteMonitorType.METRICS -> {
                metricsViewModel.onUrlLoaded(url)
            }
            SiteMonitorType.PHP_LOGS -> {
                phpLogViewModel.onUrlLoaded(url)
            }
            SiteMonitorType.WEB_SERVER_LOGS -> {
                webServerViewModel.onUrlLoaded(url)
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

    private fun trackActivityLaunched() {
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.SITE_MONITORING_SCREEN_SHOWN)
    }

    override fun onCleared() {
        super.onCleared()
        metricsViewModel.onCleared()
        phpLogViewModel.onCleared()
        webServerViewModel.onCleared()
    }
}
