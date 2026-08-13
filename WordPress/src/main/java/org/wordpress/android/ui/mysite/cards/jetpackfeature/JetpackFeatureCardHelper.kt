package org.wordpress.android.ui.mysite.cards.jetpackfeature

import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.jetpackoverlay.JETPACK_REMOVAL_TRACKING_NAME
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.PhaseThreeBlogPostLinkConfig
import java.util.Date
import javax.inject.Inject

class JetpackFeatureCardHelper @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val phaseThreeBlogPostLinkConfig: PhaseThreeBlogPostLinkConfig
) {
    fun shouldShowJetpackFeatureCard(): Boolean {
        val isWordPressApp = !buildConfigWrapper.isJetpackApp
        val exceedsShowFrequency = exceedsShowFrequencyAndResetJetpackFeatureCardLastShownTimestampIfNeeded()
        return isWordPressApp && !isJetpackCardHiddenByUser() && exceedsShowFrequency
    }

    private fun isJetpackCardHiddenByUser(): Boolean = appPrefsWrapper.getShouldHideJetpackFeatureCard()

    fun getCardContent(): UiString.UiStringRes =
        UiString.UiStringRes(R.string.jetpack_feature_card_content_phase_self_hosted_and_new_users)

    fun track(stat: Stat) {
        analyticsTrackerWrapper.track(
            stat,
            mapOf(PHASE to JETPACK_REMOVAL_TRACKING_NAME)
        )
    }

    fun getLearnMoreUrl(): String {
        val url = phaseThreeBlogPostLinkConfig.getValue<String>()

        if (url.isEmpty())
            return url

        return if (!url.contains(HOST)) {
            "$HOST$url"
        } else
            url
    }

    @Suppress("ReturnCount")
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

    fun hideJetpackFeatureCard() {
        track(Stat.REMOVE_FEATURE_CARD_HIDE_TAPPED)
        appPrefsWrapper.setShouldHideJetpackFeatureCard(true)
    }

    fun setJetpackFeatureCardLastShownTimeStamp(currentTimeMillis: Long) {
        track(Stat.REMOVE_FEATURE_CARD_REMIND_LATER_TAPPED)
        appPrefsWrapper.setJetpackFeatureCardLastShownTimestamp(currentTimeMillis)
    }
    companion object {
        const val PHASE = "phase"
        const val FREQUENCY_IN_DAYS = 4
        const val DEFAULT_LAST_SHOWN_TIMESTAMP = 0L
        const val HOST = "https://"
    }
}
