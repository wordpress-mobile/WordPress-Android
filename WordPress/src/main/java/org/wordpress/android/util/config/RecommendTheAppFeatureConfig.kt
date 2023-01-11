package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.RecommendTheAppFeatureConfig.Companion.RECOMMEND_THE_APP_FIELD
import javax.inject.Inject

@Feature(RECOMMEND_THE_APP_FIELD, true)
class RecommendTheAppFeatureConfig @Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    BuildConfig.RECOMMEND_THE_APP,
    RECOMMEND_THE_APP_FIELD
) {
    companion object {
        const val RECOMMEND_THE_APP_FIELD = "recommend_the_app_enabled"
    }
}
