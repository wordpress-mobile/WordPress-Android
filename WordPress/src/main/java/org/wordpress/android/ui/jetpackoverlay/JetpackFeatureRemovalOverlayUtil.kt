package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayPhase.PHASE_ONE
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayPhase.PHASE_THREE
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayPhase.PHASE_TWO
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType.NOTIFICATIONS
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType.READER
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureOverlayScreenType.STATS
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseNewUsers
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseOne
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseThree
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseTwo
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import javax.inject.Inject

private const val CURRENT_PHASE_KEY = "phase"
private const val SCREEN_TYPE_KEY = "source"
private const val DISMISSAL_TYPE_KEY = "dismissal_type"

@Suppress("LongParameterList", "TooManyFunctions")
class JetpackFeatureRemovalOverlayUtil @Inject constructor(
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper,
    private val jetpackFeatureOverlayShownTracker: JetpackFeatureOverlayShownTracker,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun shouldShowFeatureSpecificJetpackOverlay(feature: JetpackOverlayConnectedFeature): Boolean {
        return !buildConfigWrapper.isJetpackApp && isWpComSite() &&
                isInFeatureSpecificRemovalPhase() && hasExceededOverlayFrequency(
                feature,
                getCurrentPhasePreference()!!
        )
    }

    fun shouldShowSiteCreationOverlay(): Boolean {
        return !buildConfigWrapper.isJetpackApp && isInSiteCreationPhase()
    }

    fun shouldDisableSiteCreation(): Boolean {
        return shouldShowSiteCreationOverlay() &&
                jetpackFeatureRemovalPhaseHelper.getSiteCreationPhase() ==
                JetpackFeatureRemovalSiteCreationPhase.PHASE_TWO
    }

    private fun isInSiteCreationPhase(): Boolean {
        return when (jetpackFeatureRemovalPhaseHelper.getSiteCreationPhase()) {
            null -> false
            JetpackFeatureRemovalSiteCreationPhase.PHASE_ONE,
            JetpackFeatureRemovalSiteCreationPhase.PHASE_TWO -> true
        }
    }

    private fun isInFeatureSpecificRemovalPhase(): Boolean {
        return jetpackFeatureRemovalPhaseHelper.getCurrentPhase() != null &&
                when (jetpackFeatureRemovalPhaseHelper.getCurrentPhase()) {
                    null -> false
                    PhaseOne, PhaseTwo, PhaseThree -> true
                    PhaseFour, PhaseNewUsers -> false
                }
    }

    private fun hasExceededOverlayFrequency(
        feature: JetpackOverlayConnectedFeature,
        currentPhasePreference: JetpackFeatureRemovalOverlayPhase
    ): Boolean {
        return (hasExceededFeatureSpecificOverlayFrequency(feature, currentPhasePreference) ||
                hasExceededGlobalOverlayFrequency(currentPhasePreference))
    }

    private fun hasExceededFeatureSpecificOverlayFrequency(
        feature: JetpackOverlayConnectedFeature,
        phase: JetpackFeatureRemovalOverlayPhase
    ): Boolean {
        // Feature Overlay is never shown
        val overlayShownDate = jetpackFeatureOverlayShownTracker.getFeatureOverlayShownTimeStamp(
                feature,
                phase
        )?.let { Date(it) } ?: return true
        val daysPastOverlayShown = dateTimeUtilsWrapper.daysBetween(
                overlayShownDate,
                dateTimeUtilsWrapper.getTodaysDate()
        )
        return daysPastOverlayShown >= PhaseOne.featureSpecificOverlayFrequency
    }

    private fun hasExceededGlobalOverlayFrequency(phase: JetpackFeatureRemovalOverlayPhase): Boolean {
        // Overlay is never shown
        val overlayShownDate = jetpackFeatureOverlayShownTracker.getEarliestOverlayShownTime(phase)
                ?.let { Date(it) } ?: return true
        val daysPastOverlayShown = dateTimeUtilsWrapper.daysBetween(
                overlayShownDate,
                dateTimeUtilsWrapper.getTodaysDate()
        )
        return daysPastOverlayShown >= PhaseOne.globalOverlayFrequency
    }

    private fun isWpComSite(): Boolean {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        return selectedSite != null && siteUtilsWrapper.isAccessedViaWPComRest(selectedSite)
    }

    private fun getCurrentPhasePreference(): JetpackFeatureRemovalOverlayPhase? {
        return when (jetpackFeatureRemovalPhaseHelper.getCurrentPhase()) {
            PhaseOne -> PHASE_ONE
            PhaseTwo -> PHASE_TWO
            PhaseThree -> PHASE_THREE
            else -> null
        }
    }

    private fun onFeatureSpecificOverlayShown(feature: JetpackOverlayConnectedFeature) {
        if (isInFeatureSpecificRemovalPhase())
            jetpackFeatureOverlayShownTracker.setFeatureOverlayShownTimeStamp(
                    feature,
                    getCurrentPhasePreference()!!,
                    System.currentTimeMillis()
            )
    }

    fun onOverlayShown(overlayScreenType: JetpackFeatureOverlayScreenType?) {
        overlayScreenType?.let {
            when (it) {
                STATS -> onFeatureSpecificOverlayShown(JetpackOverlayConnectedFeature.STATS)
                NOTIFICATIONS -> onFeatureSpecificOverlayShown(JetpackOverlayConnectedFeature.NOTIFICATIONS)
                READER -> onFeatureSpecificOverlayShown(JetpackOverlayConnectedFeature.READER)
            }
            trackOverlayShown(overlayScreenType)
        }
    }

    enum class JetpackFeatureOverlayScreenType(val trackingName: String) {
        STATS("stats"),
        NOTIFICATIONS("notifications"),
        READER("reader")
    }

    private fun trackOverlayShown(jetpackFeatureOverlayScreenType: JetpackFeatureOverlayScreenType) {
        analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.JETPACK_REMOVE_FEATURE_OVERLAY_DISPLAYED,
                mapOf(
                        CURRENT_PHASE_KEY to jetpackFeatureRemovalPhaseHelper.getCurrentPhase()?.trackingName,
                        SCREEN_TYPE_KEY to jetpackFeatureOverlayScreenType.trackingName
                )
        )
    }

    fun trackBottomSheetDismissed(
        jetpackFeatureOverlayScreenType: JetpackFeatureOverlayScreenType,
        dismissalType: JetpackOverlayDismissalType
    ) {
        analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.JETPACK_REMOVE_FEATURE_OVERLAY_DISMISSED,
                mapOf(
                        CURRENT_PHASE_KEY to jetpackFeatureRemovalPhaseHelper.getCurrentPhase()?.trackingName,
                        SCREEN_TYPE_KEY to jetpackFeatureOverlayScreenType.trackingName,
                        DISMISSAL_TYPE_KEY to dismissalType.trackingName
                )
        )
    }

    fun trackInstallJetpackTapped(screenType: JetpackFeatureOverlayScreenType) {
        analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.JETPACK_REMOVE_FEATURE_OVERLAY_BUTTON_GET_JETPACK_APP_TAPPED,
                mapOf(
                        CURRENT_PHASE_KEY to jetpackFeatureRemovalPhaseHelper.getCurrentPhase()?.trackingName,
                        SCREEN_TYPE_KEY to screenType.trackingName
                )
        )
    }

    fun trackSiteCreationOverlayShown(siteCreationSource: SiteCreationSource) {
        analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.JETPACK_REMOVE_SITE_CREATION_OVERLAY_DISPLAYED,
                mapOf(
                        CURRENT_PHASE_KEY to jetpackFeatureRemovalPhaseHelper.getSiteCreationPhase()?.trackingName,
                        SCREEN_TYPE_KEY to siteCreationSource.label
                )
        )
    }

    fun trackInstallJetpackTappedInSiteCreationOverlay(siteCreationSource: SiteCreationSource) {
        analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.JETPACK_REMOVE_SITE_CREATION_OVERLAY_BUTTON_GET_JETPACK_APP_TAPPED,
                mapOf(
                        CURRENT_PHASE_KEY to jetpackFeatureRemovalPhaseHelper.getSiteCreationPhase()?.trackingName,
                        SCREEN_TYPE_KEY to siteCreationSource.label
                )
        )
    }

    fun trackBottomSheetDismissedInSiteCreationOverlay(
        siteCreationSource: SiteCreationSource,
        dismissalType: JetpackOverlayDismissalType
    ) {
        analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.JETPACK_REMOVE_SITE_CREATION_OVERLAY_DISMISSED,
                mapOf(
                        CURRENT_PHASE_KEY to jetpackFeatureRemovalPhaseHelper.getSiteCreationPhase()?.trackingName,
                        SCREEN_TYPE_KEY to siteCreationSource.label,
                        DISMISSAL_TYPE_KEY to dismissalType.trackingName
                )
        )
    }

    fun trackDeepLinkOverlayShown() {
        analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.JETPACK_DEEP_LINK_OVERLAY_DISPLAYED,
                mapOf(
                        CURRENT_PHASE_KEY to jetpackFeatureRemovalPhaseHelper.getDeepLinkPhase()?.trackingName
                )
        )
    }

    fun trackInstallJetpackTappedInDeepLinkOverlay() {
        analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.JETPACK_DEEP_LINK_OVERLAY_BUTTON_OPEN_IN_JETPACK_APP_TAPPED,
                mapOf(
                        CURRENT_PHASE_KEY to jetpackFeatureRemovalPhaseHelper.getDeepLinkPhase()?.trackingName
                )
        )
    }

    fun trackBottomSheetDismissedInDeepLinkOverlay(
        dismissalType: JetpackOverlayDismissalType
    ) {
        analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.JETPACK_DEEP_LINK_OVERLAY_DISMISSED,
                mapOf(
                        CURRENT_PHASE_KEY to jetpackFeatureRemovalPhaseHelper.getDeepLinkPhase()?.trackingName,
                        DISMISSAL_TYPE_KEY to dismissalType.trackingName
                )
        )
    }

    enum class JetpackOverlayDismissalType(val trackingName: String) {
        CLOSE_BUTTON("close"),
        CONTINUE_BUTTON("continue")
    }
}
