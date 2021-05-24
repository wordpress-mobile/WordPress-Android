package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the Mp4Composer Video Optimizer
 */
@FeatureInDevelopment
class Mp4ComposerVideoOptimizationFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.MP4_COMPOSER_VIDEO_OPTIMIZATION
)
