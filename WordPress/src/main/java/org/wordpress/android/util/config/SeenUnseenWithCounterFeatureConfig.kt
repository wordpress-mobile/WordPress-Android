package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the Unread Posts Count and Seen Status Toggle
 */
@FeatureInDevelopment
class SeenUnseenWithCounterFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.SEEN_UNSEEN_WITH_COUNTER
)
