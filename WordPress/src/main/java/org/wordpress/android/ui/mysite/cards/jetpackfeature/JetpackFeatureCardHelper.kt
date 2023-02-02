package org.wordpress.android.ui.mysite.cards.jetpackfeature

import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseThree
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseNewUsers
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseSelfHostedUsers
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
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
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper,
    private val phaseThreeBlogPostLinkConfig: PhaseThreeBlogPostLinkConfig
) {
    fun shouldShowJetpackFeatureCard(): Boolean {
        val isWordPressApp = !buildConfigWrapper.isJetpackApp
        val exceedsShowFrequency = exceedsShowFrequencyAndResetJetpackFeatureCardLastShownTimestampIfNeeded()
        return isWordPressApp && shouldShowJetpackFeatureCardInCurrentPhase() &&
                !isJetpackCardHiddenByUser() && exceedsShowFrequency
    }

    private fun isJetpackCardHiddenByUser(): Boolean {
        return jetpackFeatureRemovalPhaseHelper.getCurrentPhase()?.run {
            appPrefsWrapper.getShouldHideJetpackFeatureCard(
                this
            )
        } ?: false
    }

    fun shouldShowFeatureCardAtTop(): Boolean {
        return when (jetpackFeatureRemovalPhaseHelper.getCurrentPhase()) {
            is PhaseThree, PhaseSelfHostedUsers -> true
            else -> false
        }
    }

    private fun shouldShowJetpackFeatureCardInCurrentPhase(): Boolean {
        return when (jetpackFeatureRemovalPhaseHelper.getCurrentPhase()) {
            is PhaseThree, PhaseNewUsers, PhaseSelfHostedUsers -> true
            else -> false
        }
    }

    fun getCardContent(): UiString.UiStringRes? {
        return when (jetpackFeatureRemovalPhaseHelper.getCurrentPhase()) {
            is PhaseThree ->
                UiString.UiStringRes(R.string.jetpack_feature_card_content_phase_three)
            is PhaseNewUsers, PhaseSelfHostedUsers ->
                UiString.UiStringRes(R.string.jetpack_feature_card_content_phase_self_hosted_and_new_users)
            else -> null
        }
    }

    private fun isSwitchToJetpackMenuCardHiddenByUser(): Boolean {
        return jetpackFeatureRemovalPhaseHelper.getCurrentPhase()?.run {
            appPrefsWrapper.getShouldHideSwitchToJetpackMenuCard(
                this
            )
        } ?: false
    }

    fun track(stat: Stat) {
        analyticsTrackerWrapper.track(
            stat,
            mapOf(PHASE to jetpackFeatureRemovalPhaseHelper.getCurrentPhase()?.trackingName)
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
        val currentPhase = jetpackFeatureRemovalPhaseHelper.getCurrentPhase() ?: return false
        val lastShownTimestamp = appPrefsWrapper.getJetpackFeatureCardLastShownTimestamp(currentPhase)
        if (lastShownTimestamp == DEFAULT_LAST_SHOWN_TIMESTAMP) return true

        val lastShownDate = Date(lastShownTimestamp)
        val daysPastOverlayShown = dateTimeUtilsWrapper.daysBetween(
            lastShownDate,
            Date(System.currentTimeMillis())
        )

        val exceedsFrequency = daysPastOverlayShown >= FREQUENCY_IN_DAYS
        if (exceedsFrequency) {
            appPrefsWrapper.setJetpackFeatureCardLastShownTimestamp(currentPhase, DEFAULT_LAST_SHOWN_TIMESTAMP)
        }
        return exceedsFrequency
    }

    fun shouldShowSwitchToJetpackMenuCard(): Boolean {
        return shouldShowSwitchToJetpackMenuCardInCurrentPhase() &&
                exceedsShowFrequencyAndResetSwitchToJetpackMenuLastShownTimestampIfNeeded() &&
                !isSwitchToJetpackMenuCardHiddenByUser()
    }

    private fun shouldShowSwitchToJetpackMenuCardInCurrentPhase(): Boolean {
        return when (jetpackFeatureRemovalPhaseHelper.getCurrentPhase()) {
            is JetpackFeatureRemovalPhase.PhaseFour -> true
            else -> false
        }
    }

    private fun exceedsShowFrequencyAndResetSwitchToJetpackMenuLastShownTimestampIfNeeded(): Boolean {
        val lastShownTimestamp = appPrefsWrapper.getSwitchToJetpackMenuCardLastShownTimestamp()
        if (lastShownTimestamp == DEFAULT_LAST_SHOWN_TIMESTAMP) return true

        val lastShownDate = Date(lastShownTimestamp)
        val daysPastOverlayShown = dateTimeUtilsWrapper.daysBetween(
            lastShownDate,
            Date(System.currentTimeMillis())
        )

        val exceedsFrequency = daysPastOverlayShown >= FREQUENCY_IN_DAYS
        if (exceedsFrequency) {
            appPrefsWrapper.setSwitchToJetpackMenuCardLastShownTimestamp(DEFAULT_LAST_SHOWN_TIMESTAMP)
        }
        return exceedsFrequency
    }

    fun hideJetpackFeatureCard() {
        track(Stat.REMOVE_FEATURE_CARD_HIDE_TAPPED)
        jetpackFeatureRemovalPhaseHelper.getCurrentPhase()?.let {
            appPrefsWrapper.setShouldHideJetpackFeatureCard(it, true)
        }
    }

    fun setJetpackFeatureCardLastShownTimeStamp(currentTimeMillis: Long) {
        track(Stat.REMOVE_FEATURE_CARD_REMIND_LATER_TAPPED)
        jetpackFeatureRemovalPhaseHelper.getCurrentPhase()?.let {
            appPrefsWrapper.setJetpackFeatureCardLastShownTimestamp(it, currentTimeMillis)
        }
    }
    fun hideSwitchToJetpackMenuCard() {
        track(Stat.REMOVE_FEATURE_CARD_HIDE_TAPPED)
        jetpackFeatureRemovalPhaseHelper.getCurrentPhase()?.let {
            appPrefsWrapper.setShouldHideSwitchToJetpackMenuCard(it, true)
        }
    }
    companion object {
        const val PHASE = "phase"
        const val FREQUENCY_IN_DAYS = 4
        const val DEFAULT_LAST_SHOWN_TIMESTAMP = 0L
        const val HOST = "https://"
    }
}
