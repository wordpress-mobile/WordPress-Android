package org.wordpress.android.ui.mysite.cards.jetpackfeature

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import javax.inject.Inject

class JetpackFeatureCardHelper @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper
) {
    fun shouldShowJetpackFeatureCard() = showCard()

    fun track(stat: Stat) {
        analyticsTrackerWrapper.track(
                stat,
                mapOf(PHASE to jetpackFeatureRemovalPhaseHelper.getCurrentPhase()?.trackingName)
        )
    }

    private fun showCard(): Boolean {
        val isWordPressApp = !buildConfigWrapper.isJetpackApp
        val isPhase3 = jetpackFeatureRemovalPhaseHelper.getCurrentPhase() == JetpackFeatureRemovalPhase.PhaseThree
        val shouldHideJetpackFeatureCard = appPrefsWrapper.getShouldHideJetpackFeatureCard()
        val exceedsShowFrequency = exceedsShowFrequencyAndResetJetpackFeatureCardLastShownTimestampIfNeeded()
        return isWordPressApp && isPhase3 && !shouldHideJetpackFeatureCard && exceedsShowFrequency
    }

    private fun exceedsShowFrequencyAndResetJetpackFeatureCardLastShownTimestampIfNeeded(): Boolean {
        val lastShownTimestamp = appPrefsWrapper.getJetpackFeatureCardLastShownTimestamp()
        if (lastShownTimestamp == DEFAULT_LAST_SHOWN_TIMESTAMP) return true

        val lastShownDate = Date(lastShownTimestamp)
        val daysPastOverlayShown = dateTimeUtilsWrapper.daysBetween(
                lastShownDate,
                Date(System.currentTimeMillis())
        )

        val exceedsFrequency = daysPastOverlayShown >= FREQUENCY_IN_DAYS
        if (exceedsFrequency) {
            appPrefsWrapper.setJetpackFeatureCardLastShownTimestamp(DEFAULT_LAST_SHOWN_TIMESTAMP)
        }
        return exceedsFrequency
    }

    companion object {
        const val PHASE = "phase"
        const val FREQUENCY_IN_DAYS = 4
        const val DEFAULT_LAST_SHOWN_TIMESTAMP = 0L
    }
}
