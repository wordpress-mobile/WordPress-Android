package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalSiteCreationPhase.PHASE_TWO
import org.wordpress.android.ui.main.WPMainNavigationView.PageType
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.JetpackFeatureRemovalNewUsersConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalPhaseFourConfig
import org.wordpress.android.util.config.JetpackFeatureRemovalSelfHostedUsersConfig
import org.wordpress.android.util.config.PhaseFourOverlayFrequencyConfig
import javax.inject.Inject

// Class used to find the current phase
// of the Jetpack powered migration
class JetpackFeatureRemovalPhaseHelper @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val jetpackFeatureRemovalPhaseFourConfig: JetpackFeatureRemovalPhaseFourConfig,
    private val jetpackFeatureRemovalNewUsersConfig: JetpackFeatureRemovalNewUsersConfig,
    private val jetpackFeatureRemovalSelfHostedUsersConfig: JetpackFeatureRemovalSelfHostedUsersConfig,
    private val jetpackPhaseFourOverlayFrequencyConfig: PhaseFourOverlayFrequencyConfig,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun getCurrentPhase(): JetpackFeatureRemovalPhase? {
        return if (buildConfigWrapper.isJetpackApp) null
        else if (jetpackFeatureRemovalSelfHostedUsersConfig.isEnabled()) JetpackFeatureRemovalPhase.PhaseSelfHostedUsers
        else if (jetpackFeatureRemovalNewUsersConfig.isEnabled()) JetpackFeatureRemovalPhase.PhaseNewUsers
        else if (jetpackFeatureRemovalPhaseFourConfig.isEnabled()) JetpackFeatureRemovalPhase.PhaseFour
        else null
    }

    fun getDeepLinkPhase(): JetpackFeatureRemovalSiteCreationPhase? =
        getCurrentPhase()?.let { PHASE_TWO }

    fun shouldRemoveJetpackFeatures(): Boolean = getCurrentPhase() != null

    fun shouldShowJetpackPoweredEditorFeatures(): Boolean = getCurrentPhase() == null

    fun shouldShowTemplateSelectionInPages(): Boolean = getCurrentPhase() == null

    fun shouldShowPublishedPostStatsButton(): Boolean = getCurrentPhase() == null

    @JvmOverloads
    fun trackPageAccessedEventIfNeeded(pageType: PageType, site: SiteModel? = null) {
        when (pageType) {
            PageType.MY_SITE -> analyticsTrackerWrapper.track(AnalyticsTracker.Stat.MY_SITE_ACCESSED, site)
            PageType.READER -> analyticsTrackerWrapper.track(AnalyticsTracker.Stat.READER_ACCESSED)
            PageType.NOTIFS -> analyticsTrackerWrapper.track(AnalyticsTracker.Stat.NOTIFICATIONS_ACCESSED)
            PageType.ME -> analyticsTrackerWrapper.track(AnalyticsTracker.Stat.ME_ACCESSED)
        }
    }

    fun shouldShowNotifications(): Boolean = !shouldRemoveJetpackFeatures()

    fun shouldShowHelpAndSupportOnEditor(): Boolean = getCurrentPhase() == null

    fun getPhaseFourOverlayFrequency(): Int {
        return jetpackPhaseFourOverlayFrequencyConfig.getValue()
    }
}

sealed class JetpackFeatureRemovalPhase(val trackingName: String) {
    object PhaseFour : JetpackFeatureRemovalPhase("four")
    object PhaseNewUsers : JetpackFeatureRemovalPhase("new_users")
    object PhaseSelfHostedUsers : JetpackFeatureRemovalPhase("self_hosted")
}

enum class JetpackFeatureRemovalSiteCreationPhase(val trackingName: String) {
    PHASE_TWO("two")
}

enum class JetpackDeepLinkPhase(val trackingName: String) {
    ALL("all")
}
