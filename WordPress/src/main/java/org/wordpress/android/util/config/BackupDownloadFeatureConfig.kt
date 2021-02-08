package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.BackupDownloadFeatureConfig.Companion.BACKUP_DOWNLOAD_FLOW
import javax.inject.Inject

/**
 * Configuration of the 'Jetpack Backup Download' feature.
 */
@Feature(remoteField = BACKUP_DOWNLOAD_FLOW, defaultValue = true)
class BackupDownloadFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.BACKUP_DOWNLOAD_AVAILABLE,
        BACKUP_DOWNLOAD_FLOW
) {
    companion object {
        const val BACKUP_DOWNLOAD_FLOW = "backup_download_flow"
    }
}
