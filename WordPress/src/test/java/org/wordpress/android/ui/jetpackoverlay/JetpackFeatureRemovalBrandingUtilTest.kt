package org.wordpress.android.ui.jetpackoverlay

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.util.BuildConfigWrapper

@RunWith(MockitoJUnitRunner::class)
class JetpackFeatureRemovalBrandingUtilTest {
    @Mock private lateinit var buildConfigWrapper: BuildConfigWrapper
    @Mock private lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var jetpackFeatureRemovalBrandingUtil: JetpackFeatureRemovalBrandingUtil

    @Before
    fun setup() {
        jetpackFeatureRemovalBrandingUtil = JetpackFeatureRemovalBrandingUtil(
                buildConfigWrapper,
                jetpackFeatureRemovalPhaseHelper,
        )
    }

    @Test
    fun `given jetpack app, when phase one branding is checked, should return false`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        val shouldShowBranding = jetpackFeatureRemovalBrandingUtil.shouldShowPhaseOneBranding()

        assertFalse(shouldShowBranding)
    }

    @Test
    fun `given phase one, when phase one branding is checked, should return true`() {
        whenever(jetpackFeatureRemovalPhaseHelper.getCurrentPhase()).thenReturn(PhaseOne)
        val shouldShowBranding = jetpackFeatureRemovalBrandingUtil.shouldShowPhaseOneBranding()

        assertTrue(shouldShowBranding)
    }

    @Test
    fun `given phase one not started, when phase one branding is checked, should return false`() {
        whenever(jetpackFeatureRemovalPhaseHelper.getCurrentPhase()).thenReturn(null)
        val shouldShowBranding = jetpackFeatureRemovalBrandingUtil.shouldShowPhaseOneBranding()

        assertFalse(shouldShowBranding)
    }
}
