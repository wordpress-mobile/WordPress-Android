package org.wordpress.android.ui.mysite.cards.jetpackfeature

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.PhaseThreeBlogPostLinkConfig
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class JetpackFeatureCardHelperTest {
    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

    @Mock
    lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    @Mock
    lateinit var phaseThreeBlogPostLinkConfig: PhaseThreeBlogPostLinkConfig

    private lateinit var helper: JetpackFeatureCardHelper

    @Before
    fun setUp() {
        helper = JetpackFeatureCardHelper(
            analyticsTrackerWrapper,
            appPrefsWrapper,
            buildConfigWrapper,
            dateTimeUtilsWrapper,
            jetpackFeatureRemovalPhaseHelper,
            phaseThreeBlogPostLinkConfig
        )
    }

    @Test
    fun `when jetpack app, then jetpack feature card should not show`() {
        setTest(isJetpackApp = true)

        val result = helper.shouldShowJetpackFeatureCard()

        assertThat(result).isFalse
    }

    @Test
    fun `when not third phase, then jetpack feature card should not show`() {
        setTest(phase = JetpackFeatureRemovalPhase.PhaseTwo)

        val result = helper.shouldShowJetpackFeatureCard()

        assertThat(result).isFalse
    }

    @Test
    fun `when phase three, then jetpack feature card should show`() {
        setTest(phase = JetpackFeatureRemovalPhase.PhaseThree)

        val result = helper.shouldShowJetpackFeatureCard()

        assertThat(result).isTrue
    }

    @Test
    fun `when hide card has been set, then jetpack feature card should not shown`() {
        setTest(isCardHiddenByUser = true)

        val result = helper.shouldShowJetpackFeatureCard()

        assertThat(result).isFalse
    }

    @Test
    fun `given remind later is set, when shown frequency is not exceeded, then jetpack feature card is not shown`() {
        setTest(lastShownTimestamp = getDateXDaysAgoInMilliseconds(1))

        val result = helper.shouldShowJetpackFeatureCard()

        assertThat(result).isFalse
    }

    @Test
    fun `given remind later is set, when shown frequency is exceeded, then jetpack feature card is shown`() {
        setTest(lastShownTimestamp = getDateXDaysAgoInMilliseconds(9))

        val result = helper.shouldShowJetpackFeatureCard()

        assertThat(result).isTrue
    }

    private fun setTest(
        phase: JetpackFeatureRemovalPhase = JetpackFeatureRemovalPhase.PhaseThree,
        isJetpackApp: Boolean = false,
        isCardHiddenByUser: Boolean = false,
        lastShownTimestamp: Long = 0L
    ) {
        setPhase(phase)
        setIsJetpackApp(isJetpackApp)
        setIsCardHiddenByUser(phase, isCardHiddenByUser)
        setLastShownTimestamp(phase, lastShownTimestamp)
        setDaysBetween(lastShownTimestamp)
    }

    // Helpers
    private fun setPhase(value: JetpackFeatureRemovalPhase) {
        whenever(jetpackFeatureRemovalPhaseHelper.getCurrentPhase()).thenReturn(value)
    }

    private fun setIsJetpackApp(value: Boolean) {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(value)
    }

    private fun setIsCardHiddenByUser(phase: JetpackFeatureRemovalPhase, value: Boolean) {
        whenever(appPrefsWrapper.getShouldHideJetpackFeatureCard(phase)).thenReturn(value)
    }

    private fun setLastShownTimestamp(phase: JetpackFeatureRemovalPhase, value: Long) {
        whenever(appPrefsWrapper.getJetpackFeatureCardLastShownTimestamp(phase)).thenReturn(value)
    }

    private fun setDaysBetween(lastShownTimestamp: Long) {
        val between =
            DateTimeUtils.daysBetween(Date(lastShownTimestamp), Date(getDateXDaysAgoInMilliseconds(0)))

        whenever(dateTimeUtilsWrapper.daysBetween(any(), any())).thenReturn(between)
    }

    private fun getDateXDaysAgoInMilliseconds(daysAgo: Int) =
        System.currentTimeMillis().minus(DAY_IN_MILLISECONDS * daysAgo)

    companion object {
        private const val DAY_IN_MILLISECONDS = 86400000
    }
}
