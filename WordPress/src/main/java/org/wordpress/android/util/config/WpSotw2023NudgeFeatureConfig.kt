package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val WP_SOTW_2023_NUDGE_REMOTE_FIELD = "wp_sotw_2023_nudge"

@Feature(WP_SOTW_2023_NUDGE_REMOTE_FIELD, false)
class WpSotw2023NudgeFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.BLOGANUARY_DASHBOARD_NUDGE,
    WP_SOTW_2023_NUDGE_REMOTE_FIELD
) {
    override fun isEnabled(): Boolean {
        return super.isEnabled() && !BuildConfig.IS_JETPACK_APP
    }
}
