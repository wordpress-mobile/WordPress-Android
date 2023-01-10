package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.BloggingRemindersFeatureConfig.Companion.BLOGGING_REMINDERS_REMOTE_FIELD
import javax.inject.Inject

@Feature(BLOGGING_REMINDERS_REMOTE_FIELD, true)
class BloggingRemindersFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    BuildConfig.BLOGGING_REMINDERS,
    BLOGGING_REMINDERS_REMOTE_FIELD
) {
    companion object {
        const val BLOGGING_REMINDERS_REMOTE_FIELD = "blogging_reminders_remote_field"
    }
}
