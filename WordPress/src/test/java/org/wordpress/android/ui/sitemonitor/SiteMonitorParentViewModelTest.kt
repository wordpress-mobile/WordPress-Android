package org.wordpress.android.ui.sitemonitor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteMonitorParentViewModelTest: BaseUnitTest(){
    @Mock
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var viewModel: SiteMonitorParentViewModel

    @Before
    fun setUp() {
        viewModel = SiteMonitorParentViewModel(testDispatcher(), analyticsTrackerWrapper)
    }

    @Test
    fun `when viewmodel is started, then screen shown tracking is done`() {
        val site = mock<SiteModel>()
        viewModel.start(site)

        verify(analyticsTrackerWrapper).track(AnalyticsTracker.Stat.SITE_MONITORING_SCREEN_SHOWN)
    }
}
