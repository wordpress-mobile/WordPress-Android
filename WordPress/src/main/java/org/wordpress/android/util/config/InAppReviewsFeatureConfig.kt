package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val IN_APP_REVIEWS_REMOTE_FIELD = "in_app_reviews"

@Feature(IN_APP_REVIEWS_REMOTE_FIELD, false)
class InAppReviewsFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.IN_APP_REVIEWS,
    IN_APP_REVIEWS_REMOTE_FIELD
)
