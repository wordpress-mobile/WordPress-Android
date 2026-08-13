package org.wordpress.android.ui.jetpackoverlay

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseNewUsers
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseSelfHostedUsers
import org.wordpress.android.ui.main.WPMainNavigationView
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.JetpackFeatureRemovalNewUsersConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalPhaseFourConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalSelfHostedUsersConfig
import org.wordpress.android.util.config.PhaseFourOverlayFrequencyConfig

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class JetpackFeatureRemovalPhaseHelperTest : BaseUnitTest() {
    @Mock
    private lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    private lateinit var jetpackFeatureRemovalPhaseFourConfig: JetpackFeatureRemovalPhaseFourConfig

    @Mock
    private lateinit var jetpackFeatureRemovalNewUsersConfig: JetpackFeatureRemovalNewUsersConfig

    @Mock
    private lateinit var jetpackFeatureRemovalSelfHostedUsersConfig: JetpackFeatureRemovalSelfHostedUsersConfig

    @Mock
    private lateinit var phaseFourOverlayFrequencyConfig: PhaseFourOverlayFrequencyConfig

    @Mock
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    @Before
    fun setup() {
        jetpackFeatureRemovalPhaseHelper = JetpackFeatureRemovalPhaseHelper(
            buildConfigWrapper,
            jetpackFeatureRemovalPhaseFourConfig,
            jetpackFeatureRemovalNewUsersConfig,
            jetpackFeatureRemovalSelfHostedUsersConfig,
            phaseFourOverlayFrequencyConfig,
            analyticsTrackerWrapper
        )
    }

    // general phase tests
    @Test
    fun `given jetpack app, when current phase is fetched, then return null`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getCurrentPhase()

        assertNull(currentPhase)
    }

    @Test
    fun `given phase four config true, when current phase is fetched, then return phase four`() {
        whenever(jetpackFeatureRemovalPhaseFourConfig.isEnabled()).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getCurrentPhase()

        assertEquals(currentPhase, PhaseFour)
    }

    @Test
    fun `given phase new users config true, when current phase is fetched, then return phase new users`() {
        whenever(jetpackFeatureRemovalNewUsersConfig.isEnabled()).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getCurrentPhase()

        assertEquals(currentPhase, PhaseNewUsers)
    }

    @Test
    fun `given self hosted users config true, when current phase is fetched, then return self hosted users config`() {
        whenever(jetpackFeatureRemovalSelfHostedUsersConfig.isEnabled()).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getCurrentPhase()

        assertEquals(currentPhase, PhaseSelfHostedUsers)
    }

    @Test
    fun `when we track reader accessed event, then the proper event is tracked`() {
        jetpackFeatureRemovalPhaseHelper.trackPageAccessedEventIfNeeded(WPMainNavigationView.PageType.READER)

        verify(analyticsTrackerWrapper, times(1)).track(AnalyticsTracker.Stat.READER_ACCESSED)
    }

    @Test
    fun `when we track my site accessed event, then the proper event is tracked`() {
        val site = SiteModel()

        jetpackFeatureRemovalPhaseHelper.trackPageAccessedEventIfNeeded(WPMainNavigationView.PageType.MY_SITE, site)

        verify(analyticsTrackerWrapper, times(1)).track(AnalyticsTracker.Stat.MY_SITE_ACCESSED, site)
    }
}
