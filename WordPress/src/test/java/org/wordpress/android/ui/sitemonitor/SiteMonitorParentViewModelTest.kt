package org.wordpress.android.ui.sitemonitor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteMonitorParentViewModelTest: BaseUnitTest(){
    @Mock
    private lateinit var siteMonitorUtils: SiteMonitorUtils
    @Mock
    private lateinit var metricsViewModel: SiteMonitorTabViewModelSlice
    @Mock
    private lateinit var phpLogViewModel: SiteMonitorTabViewModelSlice
    @Mock
    private lateinit var webServerViewModel: SiteMonitorTabViewModelSlice

    private lateinit var viewModel: SiteMonitorParentViewModel

    @Before
    fun setUp() {
        viewModel = SiteMonitorParentViewModel(
            testDispatcher(),
            siteMonitorUtils,
            metricsViewModel,
            phpLogViewModel,
            webServerViewModel
        )
    }

    @Test
    fun `when viewmodel is started, then track screen shown`() {
        val site = mock<SiteModel>()
        viewModel.start(site)

        verify(siteMonitorUtils).trackActivityLaunched()
    }

    @Test
    fun `when viewmodel is created, then view model slices are initialized`() {
        verify(metricsViewModel).initialize(any())
        verify(phpLogViewModel).initialize(any())
        verify(webServerViewModel).initialize(any())
    }

    @Test
    fun `when start is invoked, then view models are started with the correct tab item`() {
        val site = mock<SiteModel>()
        viewModel.start(site)

        verify(metricsViewModel).start(SiteMonitorType.METRICS, SiteMonitorTabItem.Metrics.urlTemplate, site)
        verify(phpLogViewModel).start(SiteMonitorType.PHP_LOGS, SiteMonitorTabItem.PHPLogs.urlTemplate, site)
        verify(webServerViewModel).start(
            SiteMonitorType.WEB_SERVER_LOGS,
            SiteMonitorTabItem.WebServerLogs.urlTemplate,
            site
        )
    }

    @Test
    fun `when loadData is invoked, then view models are started with the correct tab item`() {
        val site = mock<SiteModel>()
        viewModel.start(site)

        clearInvocations(metricsViewModel, phpLogViewModel, webServerViewModel)

        viewModel.loadData()

        verify(metricsViewModel).start(SiteMonitorType.METRICS, SiteMonitorTabItem.Metrics.urlTemplate, site)
        verify(phpLogViewModel).start(SiteMonitorType.PHP_LOGS, SiteMonitorTabItem.PHPLogs.urlTemplate, site)
        verify(webServerViewModel).start(
            SiteMonitorType.WEB_SERVER_LOGS,
            SiteMonitorTabItem.WebServerLogs.urlTemplate,
            site
        )
    }

    @Test
    fun `given metrics, when getUiState is invoked, then ui state is returned`() {
        whenever(metricsViewModel.uiState).thenReturn(mock())
        val site = mock<SiteModel>()
        viewModel.start(site)

        advanceUntilIdle()

        val state = viewModel.getUiState(SiteMonitorType.METRICS)

        assertThat(state).isNotNull
    }

    @Test
    fun `given phplogs, when getUiState is invoked, then ui state is returned`() {
        whenever(phpLogViewModel.uiState).thenReturn(mock())
        val site = mock<SiteModel>()
        viewModel.start(site)

        advanceUntilIdle()

        val state = viewModel.getUiState(SiteMonitorType.PHP_LOGS)

        assertThat(state).isNotNull
    }

    @Test
    fun `given webserver logs, when getUiState is invoked, then ui state is returned`() {
        whenever(webServerViewModel.uiState).thenReturn(mock())
        val site = mock<SiteModel>()
        viewModel.start(site)

        advanceUntilIdle()

        val state = viewModel.getUiState(SiteMonitorType.WEB_SERVER_LOGS)

        assertThat(state).isNotNull
    }

    @Test
    fun `given metrics, when onUrlLoaded is invoked, then metric vm slice onUrlLoaded is invoked`() {
        viewModel.onUrlLoaded(SiteMonitorType.METRICS)

        verify(metricsViewModel).onUrlLoaded()
    }

    @Test
    fun `given php logs, when onUrlLoaded is invoked, then php logs vm slice onUrlLoaded is invoked`() {
        viewModel.onUrlLoaded(SiteMonitorType.PHP_LOGS)

        verify(phpLogViewModel).onUrlLoaded()
    }

    @Test
    fun `given webserver logs, when onUrlLoaded is invoked, then webserver logs vm slice onUrlLoaded is invoked`() {
        viewModel.onUrlLoaded(SiteMonitorType.WEB_SERVER_LOGS)

        verify(webServerViewModel).onUrlLoaded()
    }

    @Test
    fun `given metrics, when onWebViewError is invoked, then metric vm slice onWebViewError is invoked`() {
        viewModel.onWebViewError(SiteMonitorType.METRICS)

        verify(metricsViewModel).onWebViewError()
    }

    @Test
    fun `given php logs, when onWebViewError is invoked, then php logs vm slice onWebViewError is invoked`() {
        viewModel.onWebViewError(SiteMonitorType.PHP_LOGS)

        verify(phpLogViewModel).onWebViewError()
    }

    @Test
    fun `given webserver logs, when onWebViewError is invoked, then webserver vm slice onWebViewError is invoked`() {
        viewModel.onWebViewError(SiteMonitorType.WEB_SERVER_LOGS)

        verify(webServerViewModel).onWebViewError()
    }

    @Test
    fun `given metrics, when refresh is invoked, then metric vm slice refresh is invoked`() {
        viewModel.refreshData(SiteMonitorType.METRICS)

        verify(metricsViewModel).refreshData()
    }

    @Test
    fun `given php logs, when refresh is invoked, then php logs vm slice refresh is invoked`() {
        viewModel.refreshData(SiteMonitorType.PHP_LOGS)

        verify(phpLogViewModel).refreshData()
    }

    @Test
    fun `given webserver logs, when refresh is invoked, then webserver vm slice refresh is invoked`() {
        viewModel.refreshData(SiteMonitorType.WEB_SERVER_LOGS)

        verify(webServerViewModel).refreshData()
    }
}
