package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import javax.inject.Inject

class GutenbergMentionsFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(appConfig, BuildConfig.GUTENBERG_MENTIONS)
