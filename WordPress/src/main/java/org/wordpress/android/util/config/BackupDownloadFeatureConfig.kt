package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the 'Jetpack Backup Download' feature.
 */
@FeatureInDevelopment
class BackupDownloadFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.BACKUP_DOWNLOAD_AVAILABLE
)
