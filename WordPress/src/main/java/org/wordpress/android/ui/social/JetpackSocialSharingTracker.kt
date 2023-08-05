package org.wordpress.android.ui.social

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.usecase.social.JetpackSocialFlow
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JetpackSocialSharingTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun trackConnectionToggled(source: JetpackSocialFlow, value: Boolean) {
         track(
             stat = Stat.JETPACK_SOCIAL_AUTO_SHARING_CONNECTION_TOGGLED,
             properties = mapOf(
                 SOURCE to source.value,
                 VALUE to value
             )
         )
    }

    fun trackShareLimitDisplayed(source: JetpackSocialFlow) {
        track(stat = Stat.JETPACK_SOCIAL_SHARE_LIMIT_DISPLAYED, source = source)
    }

    fun trackUpgradeLinkTapped(source: JetpackSocialFlow) {
        track(stat = Stat.JETPACK_SOCIAL_UPGRADE_LINK_TAPPED, source = source)
    }

    fun trackAddConnectionCtaDisplayed(source: JetpackSocialFlow) {
        track(stat = Stat.JETPACK_SOCIAL_ADD_CONNECTION_CTA_DISPLAYED, source = source)
    }

    fun trackAddConnectionTapped(source: JetpackSocialFlow) {
        track(stat = Stat.JETPACK_SOCIAL_ADD_CONNECTION_TAPPED, source = source)
    }

    fun trackAddConnectionDismissCtaTapped(source: JetpackSocialFlow) {
        track(stat = Stat.JETPACK_SOCIAL_ADD_CONNECTION_DISMISSED, source = source)
    }

    private fun track(stat: Stat, source: JetpackSocialFlow) {
        track(
            stat = stat,
            properties = mapOf(SOURCE to source.value)
        )
    }

    private fun track(stat: Stat, properties: Map<String, Any?>) {
        analyticsTrackerWrapper.track(
            stat = stat,
            properties = properties
        )
    }

    companion object {
        private const val SOURCE = "source"
        private const val VALUE = "value"
    }
}
