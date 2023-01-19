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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
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
        whenJpDeadlineIs(null)

        val allOtherBannersAndBadges = getBrandingOnScreensWithDynamicText()

        allOtherBannersAndBadges.assertAtLeastOneMatchesEither(
            R.string.wp_jetpack_powered_phase_3_is_moving_soon,
            R.string.wp_jetpack_powered_phase_3_are_moving_soon,
        )
        verifyNoInteractions(dateTimeUtilsWrapper)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline is more than 1 month away, all other banners and badges should read {Feature} {is,are} moving soon`() {
        givenPhase(PhaseThree)
        whenJpDeadlineIs(31)

        val allOtherBannersAndBadges = getBrandingOnScreensWithDynamicText()

        allOtherBannersAndBadges.assertAtLeastOneMatchesEither(
            R.string.wp_jetpack_powered_phase_3_is_moving_soon,
            R.string.wp_jetpack_powered_phase_3_are_moving_soon,
        )
        verifyDaysUntilDeadlineCounted()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline is more than 1 week away, all other banners and badges should read {Feature} {is,are} moving in {n} weeks`() {
        givenPhase(PhaseThree)
        whenJpDeadlineIs(28)
        val expectedNumberOfWeeks = 4

        val allOtherBannersAndBadges = getBrandingOnScreensWithDynamicText()

        allOtherBannersAndBadges.assertAtLeastOneMatchesEither(
            R.string.wp_jetpack_powered_phase_3_with_deadline_is_n_weeks_away,
            R.string.wp_jetpack_powered_phase_3_with_deadline_are_n_weeks_away,
            withExpectedNumber = expectedNumberOfWeeks
        )
        verifyDaysUntilDeadlineCounted()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline is 1 week away, all other banners and badges should read {Feature} {is,are} moving in one week`() {
        givenPhase(PhaseThree)
        whenJpDeadlineIs(13)

        val allOtherBannersAndBadges = getBrandingOnScreensWithDynamicText()

        allOtherBannersAndBadges.assertAtLeastOneMatchesEither(
            R.string.wp_jetpack_powered_phase_3_with_deadline_is_one_week_away,
            R.string.wp_jetpack_powered_phase_3_with_deadline_are_one_week_away,
        )
        verifyDaysUntilDeadlineCounted()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline is more than 1 day away, all other banners and badges should read {Feature} {is,are} moving in n days`() {
        givenPhase(PhaseThree)
        whenJpDeadlineIs(6)

        val allOtherBannersAndBadges = getBrandingOnScreensWithDynamicText()

        allOtherBannersAndBadges.assertAtLeastOneMatchesEither(
            R.string.wp_jetpack_powered_phase_3_with_deadline_is_n_days_away,
            R.string.wp_jetpack_powered_phase_3_with_deadline_are_n_days_away,
            withExpectedNumber = 6
        )
        verifyDaysUntilDeadlineCounted()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline is 1 day away, all other banners and badges should read {Feature} {is,are} moving in one day`() {
        givenPhase(PhaseThree)
        whenJpDeadlineIs(1)

        val allOtherBannersAndBadges = getBrandingOnScreensWithDynamicText()

        allOtherBannersAndBadges.assertAtLeastOneMatchesEither(
            R.string.wp_jetpack_powered_phase_3_with_deadline_is_one_day_away,
            R.string.wp_jetpack_powered_phase_3_with_deadline_are_one_day_away,
        )
        verifyDaysUntilDeadlineCounted()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline is 0 days away, all other banners and badges should read Jetpack powered`() {
        givenPhase(PhaseThree)
        whenJpDeadlineIs(0)

        val allOtherBannersAndBadges = getBrandingOnScreensWithDynamicText()

        allOtherBannersAndBadges.assertAllMatch(R.string.wp_jetpack_powered)
        verifyDaysUntilDeadlineCounted()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline has passed, all other banners and badges should read Jetpack powered`() {
        givenPhase(PhaseThree)
        whenJpDeadlineIs(-15)

        val allOtherBannersAndBadges = getBrandingOnScreensWithDynamicText()

        allOtherBannersAndBadges.assertAllMatch(R.string.wp_jetpack_powered)
        verifyDaysUntilDeadlineCounted()
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

    private fun whenJpDeadlineIs(daysAway: Int?) {
        whenever(jpDeadlineConfig.appConfig.getRemoteFieldConfigValue(any())).thenReturn(daysAway?.toString())
        daysAway?.let {
            whenever(dateTimeUtilsWrapper.getTodaysDate()).thenReturn(mock())
            whenever(dateTimeUtilsWrapper.dateFromPattern(any(), any())).thenReturn(mock())
            whenever(dateTimeUtilsWrapper.daysBetween(any(), any())).thenReturn(it)
        }
    }

    private fun getBrandingOnScreensWithStaticText() = screensWithStaticText.map(classToTest::getBrandingTextByPhase)
    private fun getBrandingOnScreensWithDynamicText() = screensWithDynamicText.map(classToTest::getBrandingTextByPhase)

    private fun getAllBannersAndBadgesText(): List<UiString> {
        return getBrandingOnScreensWithStaticText() + getBrandingOnScreensWithDynamicText()
    }

    private fun List<UiString>.assertAllMatch(@StringRes expected: Int) {
        assertThat(this).allMatch { it == UiString.UiStringRes(expected) }
    }

    private fun List<UiString>.assertAtLeastOneMatchesEither(
        @StringRes expectedPlural: Int,
        @StringRes expectedSingular: Int,
        withExpectedNumber: Int? = null,
    ) {
        screensWithDynamicText.forEach { screen ->
            assertThat(this).matches { texts ->
                texts.any {
                    it == UiString.UiStringResWithParams(
                        when (screen.isFeatureNameSingular) {
                            true -> expectedPlural
                            else -> expectedSingular
                        },
                        listOfNotNull(
                            screen.featureName,
                            withExpectedNumber?.let { num -> UiString.UiStringText("$num") }
                        )
                    )
                }
            }
        }
    }

    private fun verifyDaysUntilDeadlineCounted() {
        verify(dateTimeUtilsWrapper, times(screensWithDynamicText.size)).getTodaysDate()
        verify(dateTimeUtilsWrapper, times(screensWithDynamicText.size)).dateFromPattern(
            any(),
            eq(JETPACK_OVERLAY_ORIGINAL_DATE_FORMAT)
        )
        verify(dateTimeUtilsWrapper, times(screensWithDynamicText.size)).daysBetween(any(), any())
    }
    // endregion
}
