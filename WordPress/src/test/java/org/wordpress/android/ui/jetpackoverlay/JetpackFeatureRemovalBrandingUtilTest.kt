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
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalBrandingUtil.Interval
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalBrandingUtil.Interval.Days
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalBrandingUtil.Interval.Indeterminate
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalBrandingUtil.Interval.Pluralisable
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalBrandingUtil.Interval.Soon
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalBrandingUtil.Interval.Unknown
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalBrandingUtil.Interval.Weeks
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

    private val screensWithStaticText = JetpackPoweredScreen.WithStaticText.values()
    private val screensWithDynamicText = JetpackPoweredScreen.WithDynamicText.values()
    private val allJpScreens: List<JetpackPoweredScreen> = screensWithStaticText.map { it } + screensWithDynamicText

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

        val allBannersAndBadges = allJpScreens.map {
            classToTest.getBrandingTextByPhase(it)
        }

        allBannersAndBadges.assertAllMatch(R.string.wp_jetpack_powered)
    }

    @Test
    fun `given phase one started, all banners and badges should read Jetpack powered`() {
        givenPhase(PhaseOne)

        val allBannersAndBadges = allJpScreens.map {
            classToTest.getBrandingTextByPhase(it)
        }

        allBannersAndBadges.assertAllMatch(R.string.wp_jetpack_powered)
    }

    @Test
    fun `given phase two started, all banners and badges should read Get the Jetpack app`() {
        givenPhase(PhaseTwo)

        val allBannersAndBadges = allJpScreens.map {
            classToTest.getBrandingTextByPhase(it)
        }

        allBannersAndBadges.assertAllMatch(R.string.wp_jetpack_powered_phase_2)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when on screens without dynamic text, banners and badges should read Jetpack powered`() {
        givenPhase(PhaseThree)

        val staticBannersAndBadges = screensWithStaticText.map {
            classToTest.getBrandingTextByPhase(it)
        }

        staticBannersAndBadges.assertAllMatch(R.string.wp_jetpack_powered)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline is unknown, all other banners and badges should read {Feature} {is,are} moving soon`() {
        givenPhase(PhaseThree)
        whenJpDeadlineIs(null)

        val allOtherBannersAndBadges = screensWithDynamicText.map {
            classToTest.getBrandingTextByPhase(it) as UiStringResWithParams
        }

        val actual = allOtherBannersAndBadges.match(Unknown)
        assertProperties(
            expected = allOtherBannersAndBadges,
            actual = actual,
            expectedPlural = Soon.RES_ARE_MOVING_SOON,
            expectedSingular = Soon.RES_IS_MOVING_SOON,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline is more than 1 month away, all other banners and badges should read {Feature} {is,are} moving soon`() {
        givenPhase(PhaseThree)
        whenJpDeadlineIs(32)

        val allOtherBannersAndBadges = screensWithDynamicText.map {
            classToTest.getBrandingTextByPhase(it) as UiStringResWithParams
        }

        val actual = allOtherBannersAndBadges.match(Soon)
        assertProperties(
            expected = allOtherBannersAndBadges,
            actual = actual,
            expectedPlural = Pluralisable.RES_ARE_MOVING_IN,
            expectedSingular = Pluralisable.RES_IS_MOVING_IN,
        )
        verifyDaysUntilDeadlineCounted()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline is more than 1 week away, all other banners and badges should read {Feature} {is,are} moving in {n} weeks`() {
        givenPhase(PhaseThree)
        val expectedInterval = Weeks(4)
        whenJpDeadlineIs(29)

        val allOtherBannersAndBadges = screensWithDynamicText.map {
            classToTest.getBrandingTextByPhase(it) as UiStringResWithParams
        }

        val actual = allOtherBannersAndBadges.match(expectedInterval)
        assertProperties(
            expected = allOtherBannersAndBadges,
            actual = actual,
            expectedPlural = Pluralisable.RES_ARE_MOVING_IN,
            expectedSingular = Pluralisable.RES_IS_MOVING_IN,
            expectedParams = 2,
            expectedInterval = expectedInterval,
            assertOnQuantity = ::assertQuantity,
        )
        verifyDaysUntilDeadlineCounted()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline is 1 week away, all other banners and badges should read {Feature} {is,are} moving in 1 week`() {
        givenPhase(PhaseThree)
        val expectedInterval = Weeks(1)
        whenJpDeadlineIs(13)

        val allOtherBannersAndBadges = screensWithDynamicText.map {
            classToTest.getBrandingTextByPhase(it) as UiStringResWithParams
        }

        val actual = allOtherBannersAndBadges.match(expectedInterval)
        assertProperties(
            expected = allOtherBannersAndBadges,
            actual = actual,
            expectedPlural = Pluralisable.RES_ARE_MOVING_IN,
            expectedSingular = Pluralisable.RES_IS_MOVING_IN,
            expectedParams = 2,
            expectedInterval = expectedInterval,
            assertOnQuantity = ::assertQuantity,
            )
        verifyDaysUntilDeadlineCounted()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline is more than 1 day away, all other banners and badges should read {Feature} {is,are} moving in n days`() {
        givenPhase(PhaseThree)
        val expectedInterval = Days(6)
        whenJpDeadlineIs(expectedInterval.number.toInt())

        val allOtherBannersAndBadges = screensWithDynamicText.map {
            classToTest.getBrandingTextByPhase(it) as UiStringResWithParams
        }

        val actual = allOtherBannersAndBadges.match(expectedInterval)
        assertProperties(
            expected = allOtherBannersAndBadges,
            actual = actual,
            expectedPlural = Pluralisable.RES_ARE_MOVING_IN,
            expectedSingular = Pluralisable.RES_IS_MOVING_IN,
            expectedParams = 2,
            expectedInterval = expectedInterval,
            assertOnQuantity = ::assertQuantity,
            )
        verifyDaysUntilDeadlineCounted()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline is 1 day away, all other banners and badges should read {Feature} {is,are} moving in 1 day`() {
        givenPhase(PhaseThree)
        val expectedInterval = Days(1)
        whenJpDeadlineIs(expectedInterval.number.toInt())

        val allOtherBannersAndBadges = screensWithDynamicText.map {
            classToTest.getBrandingTextByPhase(it) as UiStringResWithParams
        }

        val actual = allOtherBannersAndBadges.match(expectedInterval)
        assertProperties(
            expected = allOtherBannersAndBadges,
            actual = actual,
            expectedPlural = Pluralisable.RES_ARE_MOVING_IN,
            expectedSingular = Pluralisable.RES_IS_MOVING_IN,
            expectedParams = 2,
            expectedInterval = expectedInterval,
            assertOnQuantity = ::assertQuantity,
            )
        verifyDaysUntilDeadlineCounted()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline is 0 days away, all other banners and badges should read {Feature} {is,are} moving in 1 day`() {
        givenPhase(PhaseThree)
        val expectedInterval = Days(1)
        whenJpDeadlineIs(0)

        val allOtherBannersAndBadges = screensWithDynamicText.map {
            classToTest.getBrandingTextByPhase(it) as UiStringResWithParams
        }

        val actual = allOtherBannersAndBadges.match(expectedInterval)
        assertProperties(
            expected = allOtherBannersAndBadges,
            actual = actual,
            expectedPlural = Pluralisable.RES_ARE_MOVING_IN,
            expectedSingular = Pluralisable.RES_IS_MOVING_IN,
            expectedParams = 2,
            expectedInterval = expectedInterval,
            assertOnQuantity = ::assertQuantity,
        )
        verifyDaysUntilDeadlineCounted()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given phase three started, when deadline has passed, all other banners and badges should read Jetpack powered`() {
        givenPhase(PhaseThree)
        whenJpDeadlineIs(-15)

        val allOtherBannersAndBadges = screensWithDynamicText.map {
            classToTest.getBrandingTextByPhase(it)
        }

        allOtherBannersAndBadges.assertAllMatch(R.string.wp_jetpack_powered)
        verifyDaysUntilDeadlineCounted()
    }

    @Test
    fun `given phase four started, all banners and badges should read Jetpack powered`() {
        givenPhase(PhaseFour)

        val allBannersAndBadges = allJpScreens.map {
            classToTest.getBrandingTextByPhase(it)
        }

        allBannersAndBadges.assertAllMatch(R.string.wp_jetpack_powered)
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
            whenever(dateTimeUtilsWrapper.dateFromPattern(any(), any())).thenReturn(deadline)
        }
    }

    private fun List<UiString>.assertAllMatch(@StringRes expected: Int) {
        assertThat(this).allMatch { it == UiStringRes(expected) }
    }

    private fun List<UiStringResWithParams>.match(expectedInterval: Interval): List<UiStringResWithParams> {
        val matches = mutableListOf<UiStringResWithParams>()

        screensWithDynamicText.forEach { screen ->
            matches += filter { actual ->
                val stringRes = when (screen.isPlural) {
                    true -> when (expectedInterval) {
                        is Indeterminate -> Soon.RES_ARE_MOVING_SOON
                        is Pluralisable -> Pluralisable.RES_ARE_MOVING_IN
                        else -> error("Unexpected interval: $expectedInterval")
                    }
                    false -> when (expectedInterval) {
                        is Indeterminate -> Soon.RES_IS_MOVING_SOON
                        is Pluralisable -> Pluralisable.RES_IS_MOVING_IN
                        else -> error("Unexpected interval: $expectedInterval")
                    }
                }
                val quantityUiString = (expectedInterval as? Pluralisable)?.expectAsQuantityUiString()
                val expected = UiStringResWithParams(
                    stringRes,
                    listOfNotNull(
                        screen.featureName,
                        quantityUiString
                    )
                )

                actual == expected
            }
        }
        return matches
    }

    private fun Pluralisable.expectAsQuantityUiString() = UiStringPluralRes(
        zeroRes = otherRes,
        oneRes = oneRes,
        otherRes =otherRes,
        count = number.toInt()
    )

    private fun assertProperties(
        expected: List<UiStringResWithParams>,
        actual: List<UiStringResWithParams>,
        expectedPlural: Int,
        expectedSingular: Int,
        expectedParams: Int = 1,
        expectedInterval: Pluralisable? = null,
        assertOnQuantity: ((Pluralisable, List<UiStringPluralRes>) -> Unit)? = null,
    ) {
        assertThat(expected).containsAll(actual)

        // Assert feature name is added as param
        val params = actual.map { it.params }
        assertThat(params.flatten()).containsAll(expected.map { it.params[0] })
        assertThat(params.count { it.size == expectedParams }).isEqualTo(params.size)

        assertThat(params.flatten()).containsAll(expected.map { it.params[0] })
        assertThat(params.count { it.size == expectedParams }).isEqualTo(params.size)

        // Assert main stringRes of plurals
        val plurals = expected.map { it.stringRes }
            .filter { it == expectedPlural }
        assertThat(actual.map { it.stringRes })
            .filteredOn { it == expectedPlural }
            .hasSameElementsAs(plurals)

        // Assert main stringRes for singulars
        val singulars = expected.map { it.stringRes }
            .filter { it == expectedSingular }
        assertThat(actual.map { it.stringRes })
            .filteredOn { it == expectedSingular }
            .hasSameElementsAs(singulars)

        // Assert quantity for determinate intervals
        if (expectedParams == 2) {
            check(expectedInterval != null && assertOnQuantity != null) {
                "Pluralisation should be verified on plurals"
            }

            val quantityTexts = params.flatten().filterIsInstance<UiStringPluralRes>()
            assertOnQuantity(expectedInterval, quantityTexts) }
    }

    private fun assertQuantity(
        expectedInterval: Pluralisable,
        actualQuantity: List<UiStringPluralRes>
    ) {
        assertThat(actualQuantity.map { it.zeroRes }).containsOnly(expectedInterval.otherRes)
        assertThat(actualQuantity.map { it.oneRes }).containsOnly(expectedInterval.oneRes)
        assertThat(actualQuantity.map { it.otherRes }).containsOnly(expectedInterval.otherRes)
        assertThat(actualQuantity.map { it.count }).containsOnly(expectedInterval.number.toInt())
    }

    private fun verifyDaysUntilDeadlineCounted() {
        verify(dateTimeUtilsWrapper, times(screensWithDynamicText.size)).getTodaysDate()
        verify(dateTimeUtilsWrapper, times(screensWithDynamicText.size)).dateFromPattern(
            any(),
            eq(JETPACK_OVERLAY_ORIGINAL_DATE_FORMAT)
        )
    }
    // endregion
}
