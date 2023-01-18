package org.wordpress.android.util

import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString

@Suppress("ClassName")
sealed class JetpackBrandedScreenData(open val trackingName: String) {
    sealed class ScreenWithDynamicBranding(
        override val trackingName: String,
        val featureName: UiString? = null,
        val isFeatureNameSingular: Boolean,
    ) : JetpackBrandedScreenData(trackingName) {
        val featureVerb
            get() = when (isFeatureNameSingular) {
                true -> UiString.UiStringRes(R.string.wp_jetpack_powered_phase_3_feature_verb_singular_is)
                else -> UiString.UiStringRes(R.string.wp_jetpack_powered_phase_3_feature_verb_plural_are)
            }

        fun getBrandingTextParams(timeUntilDeadline: Int? = null) = listOfNotNull(
            featureName,
            featureVerb,
            timeUntilDeadline?.let { UiString.UiStringText("$it") },
        )
    }

    object APP_SETTINGS : JetpackBrandedScreenData(trackingName = "app_settings")
    object HOME : JetpackBrandedScreenData(trackingName = "home")
    object ME : JetpackBrandedScreenData(trackingName = "me")
    object PEOPLE : JetpackBrandedScreenData(trackingName = "people")
    object PERSON : JetpackBrandedScreenData(trackingName = "person")

    object ACTIVITY_LOG : ScreenWithDynamicBranding(
        trackingName = "activity_log",
        featureName = UiString.UiStringRes(R.string.activity_log),
        isFeatureNameSingular = true,
    )

    object ACTIVITY_LOG_DETAIL : ScreenWithDynamicBranding(
        trackingName = "activity_log_detail",
        featureName = UiString.UiStringRes(R.string.activity_log),
        isFeatureNameSingular = true,
    )

    object BACKUP : ScreenWithDynamicBranding(
        trackingName = "backup",
        featureName = UiString.UiStringRes(R.string.backup),
        isFeatureNameSingular = true,
    )

    object NOTIFICATIONS : ScreenWithDynamicBranding(
        trackingName = "notifications",
        featureName = UiString.UiStringRes(R.string.notifications_screen_title),
        isFeatureNameSingular = false,
    )

    object NOTIFICATIONS_SETTINGS : ScreenWithDynamicBranding(
        trackingName = "notifications_settings",
        featureName = UiString.UiStringRes(R.string.notification_settings),
        isFeatureNameSingular = false,
    )

    object READER : ScreenWithDynamicBranding(
        "reader",
        featureName = UiString.UiStringRes(R.string.reader_screen_title),
        isFeatureNameSingular = true,
    )

    object READER_POST_DETAIL : ScreenWithDynamicBranding(
        "reader_post_detail",
        featureName = UiString.UiStringRes(R.string.reader_screen_title),
        isFeatureNameSingular = true,
    )

    object READER_SEARCH : ScreenWithDynamicBranding(
        "reader_search",
        featureName = UiString.UiStringRes(R.string.reader_screen_title),
        isFeatureNameSingular = true,
    )

    object SHARE : ScreenWithDynamicBranding(
        "share",
        featureName = UiString.UiStringRes(R.string.my_site_btn_sharing),
        isFeatureNameSingular = true,
    )

    object STATS : ScreenWithDynamicBranding(
        "stats",
        featureName = UiString.UiStringRes(R.string.stats),
        isFeatureNameSingular = false,
    )

    object SCAN : ScreenWithDynamicBranding(
        "scan",
        featureName = UiString.UiStringRes(R.string.scan),
        isFeatureNameSingular = true,
    )

    object THEMES : ScreenWithDynamicBranding(
        "themes",
        featureName = UiString.UiStringRes(R.string.themes),
        isFeatureNameSingular = false,
    )
}
