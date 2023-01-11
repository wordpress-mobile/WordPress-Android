package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

/**
 * This prevents WP notifications when the WP app and JP app is installed.
 */
@Feature(PreventDuplicateNotifsFeatureConfig.PREVENT_DUPLICATE_NOTIFS_REMOTE_FIELD, false)
class PreventDuplicateNotifsFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.PREVENT_DUPLICATE_NOTIFS_REMOTE_FIELD,
    PREVENT_DUPLICATE_NOTIFS_REMOTE_FIELD
) {
    companion object {
        const val PREVENT_DUPLICATE_NOTIFS_REMOTE_FIELD = "prevent_duplicate_notifs_remote_field"
    }
}
