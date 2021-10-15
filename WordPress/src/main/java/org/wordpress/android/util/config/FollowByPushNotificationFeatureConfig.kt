package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.FollowByPushNotificationFeatureConfig.Companion.FOLLOW_BY_PUSH_NOTIFICATION_REMOTE_FIELD
import javax.inject.Inject

@Feature(FOLLOW_BY_PUSH_NOTIFICATION_REMOTE_FIELD, true)
class FollowByPushNotificationFeatureConfig @Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.FOLLOW_BY_PUSH_NOTIFICATION,
        FOLLOW_BY_PUSH_NOTIFICATION_REMOTE_FIELD
) {
    companion object {
        const val FOLLOW_BY_PUSH_NOTIFICATION_REMOTE_FIELD = "follow_by_push_notification_remote_field"
    }
}
