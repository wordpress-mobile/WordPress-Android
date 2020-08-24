package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import javax.inject.Inject

class WPStoriesFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(appConfig, BuildConfig.WP_STORIES_AVAILABLE)
