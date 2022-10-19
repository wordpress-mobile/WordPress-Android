package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.JetpackBloggingRemindersSyncFeatureConfig.Companion.JETPACK_BLOGGING_REMINDERS_SYNC_REMOTE_FIELD
import javax.inject.Inject

@Feature(JETPACK_BLOGGING_REMINDERS_SYNC_REMOTE_FIELD, false)
class JetpackBloggingRemindersSyncFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.JETPACK_BLOGGING_REMINDERS_SYNC,
        JETPACK_BLOGGING_REMINDERS_SYNC_REMOTE_FIELD
) {
    companion object {
        const val JETPACK_BLOGGING_REMINDERS_SYNC_REMOTE_FIELD = "jetpack_blogging_reminders_sync_remote_field"
    }
}
