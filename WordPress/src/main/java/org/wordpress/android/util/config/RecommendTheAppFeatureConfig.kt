package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

@FeatureInDevelopment
class RecommendTheAppFeatureConfig @Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.RECOMMEND_THE_APP
)
