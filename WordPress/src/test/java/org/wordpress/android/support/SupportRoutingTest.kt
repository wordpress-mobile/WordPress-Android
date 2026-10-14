package org.wordpress.android.support

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.config.NewMobileSupportFlowFeatureConfig
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class SupportRoutingTest {
    @Mock
    lateinit var experimentalFeatures: ExperimentalFeatures

    @Mock
    lateinit var newMobileSupportFlowFeatureConfig: NewMobileSupportFlowFeatureConfig

    private lateinit var supportRouting: SupportRouting

    @Before
    fun setUp() {
        supportRouting = SupportRouting(experimentalFeatures, newMobileSupportFlowFeatureConfig)
    }

    @Test
    fun `the new flow is enabled when the remote flag is on`() {
        whenever(experimentalFeatures.isEnabled(Feature.MODERN_SUPPORT)).thenReturn(false)
        whenever(newMobileSupportFlowFeatureConfig.isEnabled()).thenReturn(true)

        assertTrue(supportRouting.isNewSupportFlowEnabled())
    }

    /** The experimental toggle is the escape hatch for internal users outside the rollout. */
    @Test
    fun `the new flow is enabled when the experimental toggle is on and the remote flag is off`() {
        whenever(experimentalFeatures.isEnabled(Feature.MODERN_SUPPORT)).thenReturn(true)

        assertTrue(supportRouting.isNewSupportFlowEnabled())
    }

    /** The experimental toggle short-circuits, so a user who opted in never depends on the rollout. */
    @Test
    fun `the remote flag is not consulted once the experimental toggle is on`() {
        whenever(experimentalFeatures.isEnabled(Feature.MODERN_SUPPORT)).thenReturn(true)

        assertTrue(supportRouting.isNewSupportFlowEnabled())
        verifyNoInteractions(newMobileSupportFlowFeatureConfig)
    }

    @Test
    fun `the new flow is disabled when both signals are off`() {
        whenever(experimentalFeatures.isEnabled(Feature.MODERN_SUPPORT)).thenReturn(false)
        whenever(newMobileSupportFlowFeatureConfig.isEnabled()).thenReturn(false)

        assertFalse(supportRouting.isNewSupportFlowEnabled())
    }
}
