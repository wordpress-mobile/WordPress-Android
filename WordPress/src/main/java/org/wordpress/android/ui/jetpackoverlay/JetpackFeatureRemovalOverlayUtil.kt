package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureCollectionOverlaySource.APP_OPEN
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseFour
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseNewUsers
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhase.PhaseSelfHostedUsers
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date
import javax.inject.Inject

private const val CURRENT_PHASE_KEY = "phase"
private const val SCREEN_TYPE_KEY = "source"
private const val DISMISSAL_TYPE_KEY = "dismissal_type"

class JetpackFeatureRemovalOverlayUtil @Inject constructor(
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper,
    private val jetpackFeatureOverlayShownTracker: JetpackFeatureOverlayShownTracker,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun shouldHideJetpackFeatures(): Boolean {
        return jetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures()
    }

    fun shouldShowFeatureCollectionJetpackOverlayForFirstTime(): Boolean {
        val phase = jetpackFeatureRemovalPhaseHelper.getCurrentPhase() ?: return false
        return shouldShowFeatureCollectionOverlayInCurrentPhase(phase)
    }

    private fun shouldShowFeatureCollectionOverlayInCurrentPhase(phase: JetpackFeatureRemovalPhase): Boolean {
        return when (phase) {
            PhaseNewUsers, PhaseSelfHostedUsers ->
                !jetpackFeatureOverlayShownTracker.getFeatureCollectionOverlayShown(phase)
            PhaseFour -> shouldShowPhaseFourFeatureCollectionOverlay()
        }
    }

    // if the overlay is not shown, then show it
    // if the overlay is shown and the remote config value is 0, then don't show
    // if the overlay is shown and the remote config value is not 0, then check the frequency
    @Suppress("ReturnCount")
    private fun shouldShowPhaseFourFeatureCollectionOverlay(): Boolean {
        val isOverlayShown = jetpackFeatureOverlayShownTracker.getFeatureCollectionOverlayShown(PhaseFour)
        if (!isOverlayShown) return true
        val phaseFourOverlayFrequency = jetpackFeatureRemovalPhaseHelper.getPhaseFourOverlayFrequency()
        if (phaseFourOverlayFrequency == -1) return false
        val overlayShownDate = jetpackFeatureOverlayShownTracker.getPhaseFourOverlayShownTimeStamp()
        if (overlayShownDate != null) {
            val daysPastOverlayShown = dateTimeUtilsWrapper.daysBetween(
                Date(overlayShownDate),
                dateTimeUtilsWrapper.getTodaysDate()
            )
            return daysPastOverlayShown >= phaseFourOverlayFrequency
        }
        return false
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

    fun onFeatureCollectionOverlayShown(source: JetpackFeatureCollectionOverlaySource) {
        if (source == APP_OPEN) {
            jetpackFeatureOverlayShownTracker.setFeatureCollectionOverlayShown(
                jetpackFeatureRemovalPhaseHelper.getCurrentPhase()!!
            )
        }
        trackFeatureCollectionOverlayShown(source)
    }

    private fun trackFeatureCollectionOverlayShown(source: JetpackFeatureCollectionOverlaySource) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.JETPACK_REMOVE_FEATURE_OVERLAY_DISPLAYED,
            mapOf(
                CURRENT_PHASE_KEY to jetpackFeatureRemovalPhaseHelper.getCurrentPhase()?.trackingName,
                SCREEN_TYPE_KEY to source.label
            )
        )
    }

    fun trackBottomSheetDismissedInFeatureCollectionOverlay(
        source: JetpackFeatureCollectionOverlaySource,
        dismissalType: JetpackOverlayDismissalType
    ) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.JETPACK_REMOVE_FEATURE_OVERLAY_DISMISSED,
            mapOf(
                CURRENT_PHASE_KEY to jetpackFeatureRemovalPhaseHelper.getCurrentPhase()?.trackingName,
                SCREEN_TYPE_KEY to source.label,
                DISMISSAL_TYPE_KEY to dismissalType.trackingName
            )
        )
    }

    fun trackInstallJetpackTappedInFeatureCollectionOverlay(source: JetpackFeatureCollectionOverlaySource) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.JETPACK_REMOVE_FEATURE_OVERLAY_BUTTON_GET_JETPACK_APP_TAPPED,
            mapOf(
                CURRENT_PHASE_KEY to jetpackFeatureRemovalPhaseHelper.getCurrentPhase()?.trackingName,
                SCREEN_TYPE_KEY to source.label
            )
        )
    }

    fun trackLearnMoreAboutMigrationClickedInFeatureCollectionOverlay(source: JetpackFeatureCollectionOverlaySource) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.JETPACK_REMOVE_FEATURE_OVERLAY_LEARN_MORE_TAPPED,
            mapOf(
                CURRENT_PHASE_KEY to jetpackFeatureRemovalPhaseHelper.getCurrentPhase()?.trackingName,
                SCREEN_TYPE_KEY to source.label
            )
        )
    }

    enum class JetpackOverlayDismissalType(val trackingName: String) {
        CLOSE_BUTTON("close"),
        CONTINUE_BUTTON("continue")
    }

    enum class JetpackFeatureCollectionOverlaySource(val label: String) {
        FEATURE_CARD("card"),
        APP_OPEN("app_open"),
        DISABLED_ENTRY_POINT("disabled_entry_point"),
        UNSPECIFIED("unspecified");

        companion object {
            @JvmStatic
            fun fromString(label: String?): JetpackFeatureCollectionOverlaySource {
                return when (label) {
                    FEATURE_CARD.label -> FEATURE_CARD
                    APP_OPEN.label -> APP_OPEN
                    else -> UNSPECIFIED
                }
            }
        }
    }
}
