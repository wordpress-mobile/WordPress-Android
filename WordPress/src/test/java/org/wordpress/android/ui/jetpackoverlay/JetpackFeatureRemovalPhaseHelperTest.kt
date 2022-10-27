package org.wordpress.android.ui.jetpackoverlay

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalSiteCreationPhase.PHASE_ONE
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalSiteCreationPhase.PHASE_TWO
import org.wordpress.android.ui.jetpackoverlay.JetpackPoweredCurrentPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackPoweredCurrentPhase.PhaseNewUsers
import org.wordpress.android.ui.jetpackoverlay.JetpackPoweredCurrentPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackPoweredCurrentPhase.PhaseThree
import org.wordpress.android.ui.jetpackoverlay.JetpackPoweredCurrentPhase.PhaseTwo
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.JetpackFeatureRemovalNewUsersConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalPhaseFourConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalPhaseOneConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalPhaseThreeConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalPhaseTwoConfig

@RunWith(MockitoJUnitRunner::class)
class JetpackFeatureRemovalPhaseHelperTest {
    @Mock private lateinit var buildConfigWrapper: BuildConfigWrapper
    @Mock private lateinit var jetpackFeatureRemovalPhaseOneConfig: JetpackFeatureRemovalPhaseOneConfig
    @Mock private lateinit var jetpackFeatureRemovalPhaseTwoConfig: JetpackFeatureRemovalPhaseTwoConfig
    @Mock private lateinit var jetpackFeatureRemovalPhaseThreeConfig: JetpackFeatureRemovalPhaseThreeConfig
    @Mock private lateinit var jetpackFeatureRemovalPhaseFourConfig: JetpackFeatureRemovalPhaseFourConfig
    @Mock private lateinit var jetpackFeatureRemovalNewUsersConfig: JetpackFeatureRemovalNewUsersConfig

    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    @Before
    fun setup() {
        jetpackFeatureRemovalPhaseHelper = JetpackFeatureRemovalPhaseHelper(
                buildConfigWrapper,
                jetpackFeatureRemovalPhaseOneConfig,
                jetpackFeatureRemovalPhaseTwoConfig,
                jetpackFeatureRemovalPhaseThreeConfig,
                jetpackFeatureRemovalPhaseFourConfig,
                jetpackFeatureRemovalNewUsersConfig
        )
    }

    // general phase tests
    @Test
    fun `given jetpack app, when current phase is fetched, then return null`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getTheCurrentPhase()

        assertNull(currentPhase)
    }

    @Test
    fun `given phase one config true, when current phase is fetched, then return phase one`() {
        whenever(jetpackFeatureRemovalPhaseOneConfig.isEnabled()).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getTheCurrentPhase()

        assertEquals(currentPhase, PhaseOne)
    }

    @Test
    fun `given phase two config true, when current phase is fetched, then return phase two`() {
        whenever(jetpackFeatureRemovalPhaseTwoConfig.isEnabled()).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getTheCurrentPhase()

        assertEquals(currentPhase, PhaseTwo)
    }

    @Test
    fun `given phase three config true, when current phase is fetched, then return phase three`() {
        whenever(jetpackFeatureRemovalPhaseThreeConfig.isEnabled()).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getTheCurrentPhase()

        assertEquals(currentPhase, PhaseThree)
    }

    @Test
    fun `given phase four config true, when current phase is fetched, then return phase four`() {
        whenever(jetpackFeatureRemovalPhaseFourConfig.isEnabled()).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getTheCurrentPhase()

        assertEquals(currentPhase, PhaseFour)
    }

    @Test
    fun `given phase new users config true, when current phase is fetched, then return phase new users`() {
        whenever(jetpackFeatureRemovalNewUsersConfig.isEnabled()).thenReturn(true)

        val currentPhase = jetpackFeatureRemovalPhaseHelper.getTheCurrentPhase()

        assertEquals(currentPhase, PhaseNewUsers)
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
}
