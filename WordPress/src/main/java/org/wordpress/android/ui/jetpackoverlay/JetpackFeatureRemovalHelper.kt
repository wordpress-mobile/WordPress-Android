package org.wordpress.android.ui.jetpackoverlay

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.main.WPMainNavigationView.PageType
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

/**
 * Identifies the final Jetpack feature-removal phase in analytics and in stored preference keys.
 *
 * The removal used to be a staged rollout driven by `jp_removal_*` remote flags, and both the Tracks
 * `phase` property and several SharedPrefs keys embed the phase name. The rollout finished, so the
 * flags are gone, but the literal is kept so events stay comparable and so users who dismissed a
 * card do not see it reappear.
 */
const val JETPACK_REMOVAL_TRACKING_NAME = "self_hosted"

/**
 * The value deep-link overlay events report for the Tracks `phase` property. These events used a
 * coarser two-bucket view of the same rollout than [JETPACK_REMOVAL_TRACKING_NAME] does: "one"
 * while the Jetpack-powered features were still present, "two" once they were removed. Kept
 * verbatim so historic events stay comparable.
 */
const val JETPACK_DEEPLINK_TRACKING_NAME = "two"

/**
 * The WordPress app does not ship the Jetpack-powered features (Reader, Stats, Notifications); the
 * Jetpack app does. That is now purely a build-flavor difference.
 *
 * This used to be decided at runtime from the `jp_removal_*` remote flags. Those flags were a
 * server-side constant, and because the in-app default was "not removed", anything that emptied the
 * flag store — signing out, a fresh install, clearing app data — un-removed the features in the
 * WordPress app until the next successful fetch. Deciding it from the build flavor removes that
 * whole class of bug.
 */
class JetpackFeatureRemovalHelper @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun shouldRemoveJetpackFeatures(): Boolean = !buildConfigWrapper.isJetpackApp

    fun shouldShowJetpackPoweredEditorFeatures(): Boolean = buildConfigWrapper.isJetpackApp

    fun shouldShowTemplateSelectionInPages(): Boolean = buildConfigWrapper.isJetpackApp

    fun shouldShowPublishedPostStatsButton(): Boolean = buildConfigWrapper.isJetpackApp

    fun shouldShowNotifications(): Boolean = buildConfigWrapper.isJetpackApp

    fun shouldShowHelpAndSupportOnEditor(): Boolean = buildConfigWrapper.isJetpackApp

    fun getDeepLinkTrackingName(): String? =
        if (buildConfigWrapper.isJetpackApp) null else JETPACK_DEEPLINK_TRACKING_NAME

    @JvmOverloads
    fun trackPageAccessedEventIfNeeded(pageType: PageType, site: SiteModel? = null) {
        when (pageType) {
            PageType.MY_SITE -> analyticsTrackerWrapper.track(AnalyticsTracker.Stat.MY_SITE_ACCESSED, site)
            PageType.READER -> analyticsTrackerWrapper.track(AnalyticsTracker.Stat.READER_ACCESSED)
            PageType.NOTIFS -> analyticsTrackerWrapper.track(AnalyticsTracker.Stat.NOTIFICATIONS_ACCESSED)
            PageType.ME -> analyticsTrackerWrapper.track(AnalyticsTracker.Stat.ME_ACCESSED)
        }
    }
}
