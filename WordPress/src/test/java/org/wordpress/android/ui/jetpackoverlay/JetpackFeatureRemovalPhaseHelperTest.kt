package org.wordpress.android.ui.jetpackoverlay

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseNewUsers
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseThree
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseTwo
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseSelfHostedUsers
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalSiteCreationPhase.PHASE_ONE
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalSiteCreationPhase.PHASE_TWO
import org.wordpress.android.ui.main.WPMainNavigationView
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.JetpackFeatureRemovalNewUsersConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalPhaseFourConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalPhaseOneConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalPhaseThreeConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalPhaseTwoConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalSelfHostedUsersConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalStaticPostersConfig
import org.wordpress.android.util.config.PhaseFourOverlayFrequencyConfig

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class JetpackFeatureRemovalPhaseHelperTest : BaseUnitTest() {
    @Mock
    private lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    private lateinit var jetpackFeatureRemovalPhaseOneConfig: JetpackFeatureRemovalPhaseOneConfig

    @Mock
    private lateinit var jetpackFeatureRemovalPhaseTwoConfig: JetpackFeatureRemovalPhaseTwoConfig

    @Mock
    private lateinit var jetpackFeatureRemovalPhaseThreeConfig: JetpackFeatureRemovalPhaseThreeConfig

    @Mock
    private lateinit var jetpackFeatureRemovalPhaseFourConfig: JetpackFeatureRemovalPhaseFourConfig

    @Mock
    private lateinit var jetpackFeatureRemovalNewUsersConfig: JetpackFeatureRemovalNewUsersConfig

    @Mock
    private lateinit var jetpackFeatureRemovalSelfHostedUsersConfig: JetpackFeatureRemovalSelfHostedUsersConfig

    @Mock
    private lateinit var jetpackFeatureRemovalStaticPostersConfig: JetpackFeatureRemovalStaticPostersConfig

    @Mock
    private lateinit var phaseFourOverlayFrequencyConfig: PhaseFourOverlayFrequencyConfig

    @Mock
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    @Before
    fun setup() {
        jetpackFeatureRemovalPhaseHelper = JetpackFeatureRemovalPhaseHelper(
            buildConfigWrapper,
            jetpackFeatureRemovalPhaseOneConfig,
            jetpackFeatureRemovalPhaseTwoConfig,
            jetpackFeatureRemovalPhaseThreeConfig,
            jetpackFeatureRemovalPhaseFourConfig,
            jetpackFeatureRemovalNewUsersConfig,
            jetpackFeatureRemovalSelfHostedUsersConfig,
            jetpackFeatureRemovalStaticPostersConfig,
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
    fun `given phase one config true, when current phase is fetched, then return phase one`() {
        whenever(jetpackFeatureRemovalPhaseOneConfig.isEnabled()).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getCurrentPhase()

        assertEquals(currentPhase, PhaseOne)
    }

    @Test
    fun `given phase two config true, when current phase is fetched, then return phase two`() {
        whenever(jetpackFeatureRemovalPhaseTwoConfig.isEnabled()).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getCurrentPhase()

        assertEquals(currentPhase, PhaseTwo)
    }

    @Test
    fun `given phase three config true, when current phase is fetched, then return phase three`() {
        whenever(jetpackFeatureRemovalPhaseThreeConfig.isEnabled()).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getCurrentPhase()

        assertEquals(currentPhase, PhaseThree)
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
    fun `given static posters config true, when current phase is fetched, then return static posters config`() {
        whenever(jetpackFeatureRemovalStaticPostersConfig.isEnabled()).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getCurrentPhase()

        assertEquals(currentPhase, JetpackFeatureRemovalPhase.PhaseStaticPosters)
    }

    // site creation phase tests
    @Test
    fun `given jetpack app, when current site creation phase is fetched, then return null`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getSiteCreationPhase()

        assertNull(currentPhase)
    }

    @Test
    fun `given phase one config true, when current site creation phase is fetched, then return phase one`() {
        whenever(jetpackFeatureRemovalPhaseOneConfig.isEnabled()).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getSiteCreationPhase()

        assertEquals(currentPhase, PHASE_ONE)
    }

    @Test
    fun `given phase four config true, when current site creation phase is fetched, then return phase two`() {
        whenever(jetpackFeatureRemovalNewUsersConfig.isEnabled()).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getSiteCreationPhase()

        assertEquals(currentPhase, PHASE_TWO)
    }

    @Test
    fun `given static posters config true, when current site creation phase is fetched, then return phase two`() {
        whenever(jetpackFeatureRemovalStaticPostersConfig.isEnabled()).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getSiteCreationPhase()

        assertEquals(currentPhase, PHASE_TWO)
    }

    @Test
    fun `given it is the Jetpack app, when we track reader accessed event, then the proper event is tracked`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)

        jetpackFeatureRemovalPhaseHelper.trackPageAccessedEventIfNeeded(WPMainNavigationView.PageType.READER)

        verify(analyticsTrackerWrapper, times(1)).track(AnalyticsTracker.Stat.READER_ACCESSED)
    }

    @Test
    fun `given we do not show static posters, when we track reader accessed event, then the proper event is tracked`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)
        whenever(jetpackFeatureRemovalStaticPostersConfig.isEnabled()).thenReturn(false)

        jetpackFeatureRemovalPhaseHelper.trackPageAccessedEventIfNeeded(WPMainNavigationView.PageType.READER)

        verify(analyticsTrackerWrapper, times(1)).track(AnalyticsTracker.Stat.READER_ACCESSED)
    }

    @Test
    fun `given we do show static posters, when we track reader accessed event, then the event is not tracked`() {
        whenever(jetpackFeatureRemovalStaticPostersConfig.isEnabled()).thenReturn(true)

        jetpackFeatureRemovalPhaseHelper.trackPageAccessedEventIfNeeded(WPMainNavigationView.PageType.READER)

        verify(analyticsTrackerWrapper, never()).track(AnalyticsTracker.Stat.READER_ACCESSED)
    }

    @Test
    fun `given we show static posters, when we track my site accessed event, then the proper event is tracked`() {
        val site = SiteModel()

        jetpackFeatureRemovalPhaseHelper.trackPageAccessedEventIfNeeded(WPMainNavigationView.PageType.MY_SITE, site)

        verify(analyticsTrackerWrapper, times(1)).track(AnalyticsTracker.Stat.MY_SITE_ACCESSED, site)
    }
}
