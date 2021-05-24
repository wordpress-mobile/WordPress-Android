package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the Unified Comments improvements
 */
@FeatureInDevelopment
class UnifiedCommentsListFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.UNIFIED_COMMENTS_LIST
)
