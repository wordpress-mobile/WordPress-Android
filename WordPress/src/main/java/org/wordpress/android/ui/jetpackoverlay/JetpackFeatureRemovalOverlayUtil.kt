package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureCollectionOverlaySource.APP_OPEN
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.extensions.canUseJetpackApp
import javax.inject.Inject

private const val CURRENT_PHASE_KEY = "phase"
private const val SCREEN_TYPE_KEY = "source"
private const val DISMISSAL_TYPE_KEY = "dismissal_type"

class JetpackFeatureRemovalOverlayUtil @Inject constructor(
    private val jetpackFeatureRemovalHelper: JetpackFeatureRemovalHelper,
    private val jetpackFeatureOverlayShownTracker: JetpackFeatureOverlayShownTracker,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun shouldHideJetpackFeatures(): Boolean {
        return jetpackFeatureRemovalHelper.shouldRemoveJetpackFeatures()
    }

    /**
     * The app-open overlay tells the user their site has the Jetpack plugin and that its features moved
     * to the Jetpack app, so it's only shown for a site that can actually use that app. Sites without
     * Jetpack don't mark the overlay as seen, so it still appears the first time one that can is selected.
     */
    fun shouldShowFeatureCollectionJetpackOverlayForFirstTime(site: SiteModel?): Boolean {
        return jetpackFeatureRemovalHelper.shouldRemoveJetpackFeatures() &&
                site.canUseJetpackApp() &&
                !jetpackFeatureOverlayShownTracker.getFeatureCollectionOverlayShown()
    }

    fun trackDeepLinkOverlayShown() {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.JETPACK_DEEP_LINK_OVERLAY_DISPLAYED,
            mapOf(
                CURRENT_PHASE_KEY to jetpackFeatureRemovalHelper.getDeepLinkTrackingName()
            )
        )
    }

    fun trackInstallJetpackTappedInDeepLinkOverlay() {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.JETPACK_DEEP_LINK_OVERLAY_BUTTON_OPEN_IN_JETPACK_APP_TAPPED,
            mapOf(
                CURRENT_PHASE_KEY to jetpackFeatureRemovalHelper.getDeepLinkTrackingName()
            )
        )
    }

    fun trackBottomSheetDismissedInDeepLinkOverlay(
        dismissalType: JetpackOverlayDismissalType
    ) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.JETPACK_DEEP_LINK_OVERLAY_DISMISSED,
            mapOf(
                CURRENT_PHASE_KEY to jetpackFeatureRemovalHelper.getDeepLinkTrackingName(),
                DISMISSAL_TYPE_KEY to dismissalType.trackingName
            )
        )
    }

    fun onFeatureCollectionOverlayShown(source: JetpackFeatureCollectionOverlaySource) {
        if (source == APP_OPEN) {
            jetpackFeatureOverlayShownTracker.setFeatureCollectionOverlayShown()
        }
        trackFeatureCollectionOverlayShown(source)
    }

    private fun trackFeatureCollectionOverlayShown(source: JetpackFeatureCollectionOverlaySource) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.JETPACK_REMOVE_FEATURE_OVERLAY_DISPLAYED,
            mapOf(
                CURRENT_PHASE_KEY to JETPACK_REMOVAL_TRACKING_NAME,
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
                CURRENT_PHASE_KEY to JETPACK_REMOVAL_TRACKING_NAME,
                SCREEN_TYPE_KEY to source.label,
                DISMISSAL_TYPE_KEY to dismissalType.trackingName
            )
        )
    }

    fun trackInstallJetpackTappedInFeatureCollectionOverlay(source: JetpackFeatureCollectionOverlaySource) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.JETPACK_REMOVE_FEATURE_OVERLAY_BUTTON_GET_JETPACK_APP_TAPPED,
            mapOf(
                CURRENT_PHASE_KEY to JETPACK_REMOVAL_TRACKING_NAME,
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
