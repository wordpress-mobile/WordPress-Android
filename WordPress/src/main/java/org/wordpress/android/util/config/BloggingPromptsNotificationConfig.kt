package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.BloggingPromptsNotificationConfig.Companion.BLOGGING_PROMPTS_NOTIFICATION_REMOTE_FIELD
import javax.inject.Inject

@Feature(BLOGGING_PROMPTS_NOTIFICATION_REMOTE_FIELD, false)
class BloggingPromptsNotificationConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.BLOGGING_PROMPTS_NOTIFICATION,
        BLOGGING_PROMPTS_NOTIFICATION_REMOTE_FIELD
) {
    companion object {
        const val BLOGGING_PROMPTS_NOTIFICATION_REMOTE_FIELD = "blogging_prompts_notification_remote_field"
    }
}
