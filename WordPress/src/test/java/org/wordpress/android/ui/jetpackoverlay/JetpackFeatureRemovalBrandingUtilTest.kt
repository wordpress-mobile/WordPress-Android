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
import org.wordpress.android.ui.jetpackoverlay.JetpackBrandingUiState.Days
import org.wordpress.android.ui.jetpackoverlay.JetpackBrandingUiState.Pluralisable
import org.wordpress.android.ui.jetpackoverlay.JetpackBrandingUiState.Soon
import org.wordpress.android.ui.jetpackoverlay.JetpackBrandingUiState.Weeks
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseThree
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseTwo
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringPluralRes
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.config.JPDeadlineConfigStub
import java.time.ZoneId
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class JetpackFeatureRemovalBrandingUtilTest {
    private val jpDeadlineConfig = JPDeadlineConfigStub()
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper = mock()
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper = mock()

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    private lateinit var classToTest: JetpackFeatureRemovalBrandingUtil

    private val screensWithStaticText = JetpackPoweredScreen.WithStaticText.values().toList()
    private val screensWithDynamicText = JetpackPoweredScreen.WithDynamicText.values().toList()
    private val allJpScreens: List<JetpackPoweredScreen> = screensWithStaticText + screensWithDynamicText

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
    fun `given phase 1 not started, all banners and badges should be Jetpack powered`() {
        givenPhase(null)

        val actual = allJpScreens.map(classToTest::getBrandingTextByPhase)

        actual.assertAllMatch(R.string.wp_jetpack_powered)
    }

    @Test
    fun `given phase 1 started, all banners and badges should be Jetpack powered`() {
        givenPhase(PhaseOne)

        val actual = allJpScreens.map(classToTest::getBrandingTextByPhase)

        actual.assertAllMatch(R.string.wp_jetpack_powered)
    }

    @Test
    fun `given phase 2 started, all banners and badges should be Get the Jetpack app`() {
        givenPhase(PhaseTwo)

        val actual = allJpScreens.map(classToTest::getBrandingTextByPhase)

        actual.assertAllMatch(R.string.wp_jetpack_powered_phase_2)
    }

    @Test
    fun `given phase 3 started, on screens without dynamic branding, some branding should be Jetpack powered`() {
        givenPhase(PhaseThree)

        val actual = screensWithStaticText.map(classToTest::getBrandingTextByPhase)

        actual.assertAllMatch(R.string.wp_jetpack_powered)
    }

    @Test
    fun `given phase 3 started, when deadline unknown, all other branding should be {Feature} {is,are} moving soon`() {
        givenPhase(PhaseThree)
        whenJpDeadlineIs(null)

        val actual = screensWithDynamicText.map {
            classToTest.getBrandingTextByPhase(it) as UiStringResWithParams
        }

        actual.assertDynamicTexts(
            expectedPluralRes = Soon.RES_ARE_MOVING_SOON,
            expectedSingularRes = Soon.RES_IS_MOVING_SOON,
            expectedToHaveQuantity = false,
        )
        verifyNoInteractions(dateTimeUtilsWrapper)
    }

    @Test
    fun `given phase 3 started, when deadline 32 days, all other branding should be {Feature} {is,are} moving soon`() {
        givenPhase(PhaseThree)
        whenJpDeadlineIs(32)

        val actual = screensWithDynamicText.map {
            classToTest.getBrandingTextByPhase(it) as UiStringResWithParams
        }

        actual.assertDynamicTexts(
            expectedPluralRes = Soon.RES_ARE_MOVING_SOON,
            expectedSingularRes = Soon.RES_IS_MOVING_SOON,
            expectedToHaveQuantity = false,
        )
        verifyDaysUntilDeadlineCounted()
    }

    @Test
    fun `given phase 3, when deadline in 29 days, other branding should be {Feature} {is,are} moving in {n} weeks`() {
        givenPhase(PhaseThree)
        val expectedInterval = Weeks(4)
        whenJpDeadlineIs(29)

        val actual = screensWithDynamicText.map {
            classToTest.getBrandingTextByPhase(it) as UiStringResWithParams
        }

        actual.assertDynamicTexts(
            expectedPluralRes = Pluralisable.RES_ARE_MOVING_IN,
            expectedSingularRes = Pluralisable.RES_IS_MOVING_IN,
            expectedToHaveQuantity = true,
        )
        actual.assertQuantityTexts(expectedInterval)
        verifyDaysUntilDeadlineCounted()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase 3 started, when deadline is in 1 week, all other branding should be {Feature} {is,are} moving in 1 week`() {
        givenPhase(PhaseThree)
        val expectedInterval = Weeks(1)
        whenJpDeadlineIs(13)

        val actual = screensWithDynamicText.map {
            classToTest.getBrandingTextByPhase(it) as UiStringResWithParams
        }

        actual.assertDynamicTexts(
            expectedPluralRes = Pluralisable.RES_ARE_MOVING_IN,
            expectedSingularRes = Pluralisable.RES_IS_MOVING_IN,
            expectedToHaveQuantity = true,
        )
        actual.assertQuantityTexts(expectedInterval)
        verifyDaysUntilDeadlineCounted()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase 3 started, when deadline is in 6 days, all other branding should be {Feature} {is,are} moving in n days`() {
        givenPhase(PhaseThree)
        val expectedInterval = Days(6)
        whenJpDeadlineIs(expectedInterval.number.toInt())

        val actual = screensWithDynamicText.map {
            classToTest.getBrandingTextByPhase(it) as UiStringResWithParams
        }

        actual.assertDynamicTexts(
            expectedPluralRes = Pluralisable.RES_ARE_MOVING_IN,
            expectedSingularRes = Pluralisable.RES_IS_MOVING_IN,
            expectedToHaveQuantity = true,
        )
        actual.assertQuantityTexts(expectedInterval)
        verifyDaysUntilDeadlineCounted()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase 3 started, when deadline is in 1 day, all other branding should be {Feature} {is,are} moving in 1 day`() {
        givenPhase(PhaseThree)
        val expectedInterval = Days(1)
        whenJpDeadlineIs(expectedInterval.number.toInt())

        val actual = screensWithDynamicText.map {
            classToTest.getBrandingTextByPhase(it) as UiStringResWithParams
        }

        actual.assertDynamicTexts(
            expectedPluralRes = Pluralisable.RES_ARE_MOVING_IN,
            expectedSingularRes = Pluralisable.RES_IS_MOVING_IN,
            expectedToHaveQuantity = true,
        )
        actual.assertQuantityTexts(expectedInterval)
        verifyDaysUntilDeadlineCounted()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase 3 started, when deadline is in 0 days, all other branding should be {Feature} {is,are} moving in 1 day`() {
        givenPhase(PhaseThree)
        val expectedInterval = Days(1)
        whenJpDeadlineIs(0)

        val actual = screensWithDynamicText.map {
            classToTest.getBrandingTextByPhase(it) as UiStringResWithParams
        }

        actual.assertDynamicTexts(
            expectedPluralRes = Pluralisable.RES_ARE_MOVING_IN,
            expectedSingularRes = Pluralisable.RES_IS_MOVING_IN,
            expectedToHaveQuantity = true,
        )
        actual.assertQuantityTexts(expectedInterval)
        verifyDaysUntilDeadlineCounted()
    }

    @Test
    fun `given phase 3 started, when deadline has passed, all other branding should be Jetpack powered`() {
        givenPhase(PhaseThree)
        whenJpDeadlineIs(-15)

        val actual = screensWithDynamicText.map(classToTest::getBrandingTextByPhase)

        actual.assertAllMatch(R.string.wp_jetpack_powered)
        verifyDaysUntilDeadlineCounted()
    }

    @Test
    fun `given phase 4 started, all banners and badges should be Jetpack powered`() {
        givenPhase(PhaseFour)

        val actual = allJpScreens.map(classToTest::getBrandingTextByPhase)

        actual.assertAllMatch(R.string.wp_jetpack_powered)
        verifyNoInteractions(dateTimeUtilsWrapper)
    }

    // endregion

    // region Helpers

    private fun givenPhase(phase: JetpackFeatureRemovalPhase?) {
        whenever(jetpackFeatureRemovalPhaseHelper.getCurrentPhase()).thenReturn(phase)
    }

    private fun whenJpDeadlineIs(daysAway: Int?) {
        whenever(jpDeadlineConfig.appConfig.getRemoteFieldConfigValue(any())).thenReturn(daysAway?.toString())
        daysAway?.toLong()?.let {
            val today = Date(System.currentTimeMillis())
            val deadline = Date.from(today.toInstant().atZone(ZoneId.systemDefault()).plusDays(it).toInstant())
            whenever(dateTimeUtilsWrapper.getTodaysDate()).thenReturn(today)
            whenever(dateTimeUtilsWrapper.parseDateString(any(), any())).thenReturn(deadline)
        }
    }

    private fun List<UiString>.assertAllMatch(@StringRes expected: Int) {
        assertThat(this).allMatch { it == UiStringRes(expected) }
    }

    private fun List<UiStringResWithParams>.assertDynamicTexts(
        @StringRes expectedPluralRes: Int,
        @StringRes expectedSingularRes: Int,
        expectedToHaveQuantity: Boolean,
    ) {
        val actualFeatureNames = map { it.params[0] }
        val expectedFeatureNames = screensWithDynamicText.map { it.featureName }
        assertThat(actualFeatureNames).isEqualTo(expectedFeatureNames)

        val actualStringRes = map { it.stringRes }

        // Assert plurals stringRes
        val actualPlurals = actualStringRes.filter { it == expectedPluralRes }
        assertThat(actualPlurals).containsOnly(expectedPluralRes)
        assertThat(actualPlurals).hasSameSizeAs(screensWithDynamicText.filter { it.isPlural })

        // Assert singulars stringRes
        val actualSingulars = actualStringRes.filter { it == expectedSingularRes }
        assertThat(actualSingulars).containsOnly(expectedSingularRes)
        assertThat(actualSingulars).hasSameSizeAs(screensWithDynamicText.filter { !it.isPlural })

        // Assert params
        val actualParams = map { it.params }
        assertThat(actualParams).allMatch { it.size == if (expectedToHaveQuantity) 2 else 1 }
    }

    private fun List<UiStringResWithParams>.assertQuantityTexts(
        expected: Pluralisable,
    ) {
        val actualQuantity = map { it.params[1] as UiStringPluralRes }
        assertThat(actualQuantity.map { it.zeroRes }).containsOnly(expected.otherRes)
        assertThat(actualQuantity.map { it.oneRes }).containsOnly(expected.oneRes)
        assertThat(actualQuantity.map { it.otherRes }).containsOnly(expected.otherRes)
        assertThat(actualQuantity.map { it.count }).containsOnly(expected.number.toInt())
    }

    private fun verifyDaysUntilDeadlineCounted() {
        verify(dateTimeUtilsWrapper, times(screensWithDynamicText.size)).getTodaysDate()
        verify(dateTimeUtilsWrapper, times(screensWithDynamicText.size)).parseDateString(
            any(),
            eq(JETPACK_OVERLAY_ORIGINAL_DATE_FORMAT)
        )
    }
    // endregion
}
