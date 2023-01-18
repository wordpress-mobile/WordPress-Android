package org.wordpress.android.ui.jetpackoverlay

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseTwo
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.config.JPDeadlineConfig

@RunWith(MockitoJUnitRunner::class)
class JetpackFeatureRemovalBrandingUtilTest {

    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper = mock()
    private val jpDeadlineConfig: JPDeadlineConfig = mock()
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper = mock()

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    private lateinit var classToTest: JetpackFeatureRemovalBrandingUtil

    private val screensWithStaticText = JetpackPoweredScreen.WithStaticText.values()
    private val screensWithDynamicText = JetpackPoweredScreen.WithDynamicText.values()

    @Before
    fun setup() {
        classToTest = JetpackFeatureRemovalBrandingUtil(
            jetpackFeatureRemovalPhaseHelper,
            jpDeadlineConfig,
            dateTimeUtilsWrapper,
        )
    }

    // region Branding Visibility
    @Test
    fun `given phase one started, when phase one branding is checked, should return true`() {
        givenPhase(PhaseOne)

        val shouldShowBranding = classToTest.shouldShowPhaseOneBranding()

        assertTrue(shouldShowBranding)
    }

    @Test
    fun `given phase one not started, when phase one branding is checked, should return false`() {
        givenPhase(null)
        val shouldShowBranding = classToTest.shouldShowPhaseOneBranding()

        assertFalse(shouldShowBranding)
    }

    @Test
    fun `given phase one not started, when phase two branding is checked, should return false`() {
        givenPhase(null)
        val shouldShowBranding = classToTest.shouldShowPhaseTwoBranding()

        assertFalse(shouldShowBranding)
    }

    @Test
    fun `given phase one started, when phase two branding is checked, should return false`() {
        whenever(jetpackFeatureRemovalPhaseHelper.getCurrentPhase()).thenReturn(PhaseOne)
        val shouldShowBranding = classToTest.shouldShowPhaseTwoBranding()

        assertFalse(shouldShowBranding)
    }

    @Test
    fun `given phase two started, when phase two branding is checked, should return true`() {
        whenever(jetpackFeatureRemovalPhaseHelper.getCurrentPhase()).thenReturn(PhaseTwo)
        val shouldShowBranding = classToTest.shouldShowPhaseTwoBranding()

        assertTrue(shouldShowBranding)
    }
    // endregion

    // region Branding Text

    @Test
    fun `given phase one not started, all banners and badges should read Jetpack powered`() {
        givenPhase()

        val allBannersAndBadges = getAllBannersAndBadgesText()

        assertThat(allBannersAndBadges).allMatch { it == UiString.UiStringRes(R.string.wp_jetpack_powered) }
    }

    @Test
    fun `given phase one started, all banners and badges should read Jetpack powered`() {
        givenPhase(PhaseOne)

        val allBannersAndBadges = getAllBannersAndBadgesText()

        assertThat(allBannersAndBadges).allMatch { it == UiString.UiStringRes(R.string.wp_jetpack_powered) }
    }

    @Test
    fun `given phase two started, all banners and badges should read Get the Jetpack app`() {
        givenPhase(PhaseTwo)

        val allBannersAndBadges = getAllBannersAndBadgesText()

        assertThat(allBannersAndBadges).allMatch { it == UiString.UiStringRes(R.string.wp_jetpack_powered_phase_2) }
    }

    @Test
    fun `given phase four started, all banners and badges should read Jetpack powered`() {
        givenPhase(PhaseFour)

        val allBannersAndBadges = getAllBannersAndBadgesText()

        assertThat(allBannersAndBadges).allMatch { it == UiString.UiStringRes(R.string.wp_jetpack_powered) }
    }

    // endregion

    // region Test Helpers

    private fun getBrandingOnScreensWithStaticText() = screensWithStaticText.map(classToTest::getBrandingTextByPhase)
    private fun getBrandingOnScreensWithDynamicText() = screensWithDynamicText.map(classToTest::getBrandingTextByPhase)


    private fun givenPhase(phase: JetpackFeatureRemovalPhase? = null) {
        whenever(jetpackFeatureRemovalPhaseHelper.getCurrentPhase()).thenReturn(phase)
    }

    private fun getAllBannersAndBadgesText(): List<UiString> {
        return getBrandingOnScreensWithStaticText() + getBrandingOnScreensWithDynamicText()
    }

    // endregion
}
