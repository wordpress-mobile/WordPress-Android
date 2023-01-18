package org.wordpress.android.models

import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes

sealed interface JetpackPoweredScreen {
    val trackingName: String

    sealed interface JetpackPoweredScreenWithDynamicText : JetpackPoweredScreen {
        val featureName: UiString
        val isFeatureNameSingular: Boolean
    }

    enum class WithStaticText(
        override val trackingName: String,
    ) : JetpackPoweredScreen {
        APP_SETTINGS(trackingName = "app_settings"),
        HOME(trackingName = "home"),
        ME(trackingName = "me"),
        PEOPLE(trackingName = "people"),
        PERSON(trackingName = "person"),
    }

    enum class WithDynamicText(
        override val trackingName: String,
        override val featureName: UiString,
        override val isFeatureNameSingular: Boolean,
    ): JetpackPoweredScreenWithDynamicText {
        ACTIVITY_LOG(
            trackingName = "activity_log",
            featureName = UiStringRes(R.string.activity_log),
            isFeatureNameSingular = true,
        ),
        ACTIVITY_LOG_DETAIL(
            trackingName = "activity_log_detail",
            featureName = UiStringRes(R.string.activity_log),
            isFeatureNameSingular = true,
        ),
        BACKUP(
            trackingName = "backup",
            featureName = UiStringRes(R.string.backup),
            isFeatureNameSingular = true,
        ),
        BACKUP_DETAIL(
            trackingName = "backup_detail",
            featureName = UiStringRes(R.string.backup),
            isFeatureNameSingular = true,
        ),
        NOTIFICATIONS(
            trackingName = "notifications",
            featureName = UiStringRes(R.string.notifications_screen_title),
            isFeatureNameSingular = false,
        ),
        NOTIFICATIONS_SETTINGS(
            trackingName = "notifications_settings",
            featureName = UiStringRes(R.string.notification_settings),
            isFeatureNameSingular = false,
        ),
        READER(
            "reader",
            featureName = UiStringRes(R.string.reader_screen_title),
            isFeatureNameSingular = true,
        ),
        READER_POST_DETAIL(
            "reader_post_detail",
            featureName = UiStringRes(R.string.reader_screen_title),
            isFeatureNameSingular = true,
        ),
        READER_SEARCH(
            "reader_search",
            featureName = UiStringRes(R.string.reader_screen_title),
            isFeatureNameSingular = true,
        ),
        SHARE(
            "share",
            featureName = UiStringRes(R.string.my_site_btn_sharing),
            isFeatureNameSingular = true,
        ),
        STATS(
            "stats",
            featureName = UiStringRes(R.string.stats),
            isFeatureNameSingular = false,
        ),
        SCAN(
            "scan",
            featureName = UiStringRes(R.string.scan),
            isFeatureNameSingular = true,
        ),
        THEMES(
            "themes",
            featureName = UiStringRes(R.string.themes),
            isFeatureNameSingular = false,
        );
    }
}
