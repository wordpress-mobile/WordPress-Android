package org.wordpress.android.ui.jetpackoverlay

import androidx.annotation.StringRes
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseThree
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseTwo
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.config.JPDeadlineConfigStub

@RunWith(MockitoJUnitRunner::class)
class JetpackFeatureRemovalBrandingUtilTest {

    private val jpDeadlineConfig = JPDeadlineConfigStub()
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper = mock()
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
            jpDeadlineConfig.instance,
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
        givenPhase(null)

        val allBannersAndBadges = getAllBannersAndBadgesText()

        allBannersAndBadges.assertAllMatch(R.string.wp_jetpack_powered)
    }

    @Test
    fun `given phase one started, all banners and badges should read Jetpack powered`() {
        givenPhase(PhaseOne)

        val allBannersAndBadges = getAllBannersAndBadgesText()

        allBannersAndBadges.assertAllMatch(R.string.wp_jetpack_powered)
    }

    @Test
    fun `given phase two started, all banners and badges should read Get the Jetpack app`() {
        givenPhase(PhaseTwo)

        val allBannersAndBadges = getAllBannersAndBadgesText()

        allBannersAndBadges.assertAllMatch(R.string.wp_jetpack_powered_phase_2)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when on screens without dynamic text, banners and badges should read Jetpack powered`() {
        givenPhase(PhaseThree)

        val bannersAndBadgesOnScreensWithStaticText = getBrandingOnScreensWithStaticText()

        bannersAndBadgesOnScreensWithStaticText.assertAllMatch(R.string.wp_jetpack_powered)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline is unknown, all other banners and badges should read {Feature} {is,are} moving soon`() {
        givenPhase(PhaseThree)
        whenJpDeadlineIs("test")

        val allOtherBannersAndBadges = getBrandingOnScreensWithDynamicText()

        allOtherBannersAndBadges.verifyBannersAndBadgesTextIsMovingSoon()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline is more than 1 month away on screens without dynamic text, banners and badges should read {Feature} {is,are} moving soon`() {
        givenPhase(PhaseThree)
        whenJpDeadlineIs("01-01-9999")

        val bannersAndBadgesOnScreensWithStaticText = getBrandingOnScreensWithStaticText()

        bannersAndBadgesOnScreensWithStaticText.assertAllMatch(R.string.wp_jetpack_powered)
    }

    @Test
    fun `given phase four started, all banners and badges should read Jetpack powered`() {
        givenPhase(PhaseFour)

        val allBannersAndBadges = getAllBannersAndBadgesText()

        allBannersAndBadges.assertAllMatch(R.string.wp_jetpack_powered)
    }

    // endregion

    // region Test Helpers

    private fun givenPhase(phase: JetpackFeatureRemovalPhase?) {
        whenever(jetpackFeatureRemovalPhaseHelper.getCurrentPhase()).thenReturn(phase)
    }

    private fun whenJpDeadlineIs(date: String?) {
        whenever(jpDeadlineConfig.appConfig.getRemoteFieldConfigValue(any())).thenReturn(date)
    }

    private fun getBrandingOnScreensWithStaticText() = screensWithStaticText.map(classToTest::getBrandingTextByPhase)
    private fun getBrandingOnScreensWithDynamicText() = screensWithDynamicText.map(classToTest::getBrandingTextByPhase)

    private fun getAllBannersAndBadgesText(): List<UiString> {
        return getBrandingOnScreensWithStaticText() + getBrandingOnScreensWithDynamicText()
    }

    private fun List<UiString>.assertAllMatch(@StringRes expected: Int) {
        assertThat(this).allMatch { it == UiString.UiStringRes(expected) }
    }

    private fun List<UiString>.verifyBannersAndBadgesTextIsMovingSoon() {
        screensWithDynamicText.forEach { screen ->
            assertThat(this).matches { texts ->
                texts.any {
                    it == UiString.UiStringResWithParams(
                        when (screen.isFeatureNameSingular) {
                            true -> R.string.wp_jetpack_powered_phase_3_is_moving_soon
                            else -> R.string.wp_jetpack_powered_phase_3_are_moving_soon
                        },
                        screen.featureName
                    )
                }
            }
        }
    }

    // endregion
}
