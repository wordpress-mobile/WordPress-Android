package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val BLOGANUARY_NUDGE_REMOTE_FIELD = "bloganuary_dashboard_nudge"

@Feature(BLOGANUARY_NUDGE_REMOTE_FIELD, true)
class BloganuaryNudgeFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.BLOGANUARY_DASHBOARD_NUDGE,
    BLOGANUARY_NUDGE_REMOTE_FIELD
) {
    override fun isEnabled(): Boolean {
        return super.isEnabled() && BuildConfig.IS_JETPACK_APP
    }
}
