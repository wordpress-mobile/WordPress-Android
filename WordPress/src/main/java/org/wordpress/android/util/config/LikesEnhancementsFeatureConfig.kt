package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.LikesEnhancementsFeatureConfig.Companion.LIKES_ENHANCEMENTS_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration of the Likes Enhancements feature
 */
@Feature(LIKES_ENHANCEMENTS_REMOTE_FIELD, true)
class LikesEnhancementsFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    BuildConfig.LIKES_ENHANCEMENTS,
    LIKES_ENHANCEMENTS_REMOTE_FIELD
) {
    companion object {
        const val LIKES_ENHANCEMENTS_REMOTE_FIELD = "likes_enhancements_enabled"
    }
}
