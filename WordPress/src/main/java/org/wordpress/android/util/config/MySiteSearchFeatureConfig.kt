package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the my site search
 */
@FeatureInDevelopment
class MySiteSearchFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.MY_SITE_SEARCH
)
