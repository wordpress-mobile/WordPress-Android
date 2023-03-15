package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.JetpackFeatureRemovalStaticPostersConfig.Companion.JETPACK_FEATURE_REMOVAL_STATIC_POSTERS_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration for Jetpack feature removal phase new users
 */
@Feature(JETPACK_FEATURE_REMOVAL_STATIC_POSTERS_REMOTE_FIELD, false)
class JetpackFeatureRemovalStaticPostersConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.JETPACK_FEATURE_REMOVAL_STATIC_POSTERS,
    JETPACK_FEATURE_REMOVAL_STATIC_POSTERS_REMOTE_FIELD
) {
    companion object {
        const val JETPACK_FEATURE_REMOVAL_STATIC_POSTERS_REMOTE_FIELD = "jp_removal_static_posters"
    }
}
