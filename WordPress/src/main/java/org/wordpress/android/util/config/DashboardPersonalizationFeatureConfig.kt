package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

@Feature(DashboardPersonalizationFeatureConfig.DASHBOARD_PERSONALIZATION_REMOTE_FIELD, false)
class DashboardPersonalizationFeatureConfig @Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    BuildConfig.DASHBOARD_PERSONALIZATION,
    DASHBOARD_PERSONALIZATION_REMOTE_FIELD
) {
    override fun isEnabled(): Boolean {
        return super.isEnabled() && BuildConfig.IS_JETPACK_APP
    }

    companion object {
        const val DASHBOARD_PERSONALIZATION_REMOTE_FIELD = "dashboard_personalization"
    }
}
