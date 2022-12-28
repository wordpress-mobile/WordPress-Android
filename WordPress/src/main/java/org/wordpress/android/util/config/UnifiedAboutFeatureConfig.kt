package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.UnifiedAboutFeatureConfig.Companion.UNIFIED_ABOUT_REMOTE_FIELD
import javax.inject.Inject

@Feature(UNIFIED_ABOUT_REMOTE_FIELD, true)
class UnifiedAboutFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.UNIFIED_ABOUT,
    UNIFIED_ABOUT_REMOTE_FIELD
) {
    companion object {
        const val UNIFIED_ABOUT_REMOTE_FIELD = "unified_about_enabled"
    }
}
