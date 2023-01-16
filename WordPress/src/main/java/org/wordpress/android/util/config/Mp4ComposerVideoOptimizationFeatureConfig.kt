package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.Mp4ComposerVideoOptimizationFeatureConfig.Companion.MP4_COMPOSER_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration of the Mp4Composer Video Optimizer
 */
@Feature(MP4_COMPOSER_REMOTE_FIELD, true)
class Mp4ComposerVideoOptimizationFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    BuildConfig.MP4_COMPOSER_VIDEO_OPTIMIZATION,
    MP4_COMPOSER_REMOTE_FIELD
) {
    companion object {
        const val MP4_COMPOSER_REMOTE_FIELD = "mp4_composer_video_optimization_enabled"
    }
}
