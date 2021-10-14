package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

@FeatureInDevelopment
class FollowByPushNotificationFeatureConfig @Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.FOLLOW_BY_PUSH_NOTIFICATION
)
