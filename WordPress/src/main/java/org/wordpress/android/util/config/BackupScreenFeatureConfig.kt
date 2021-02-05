package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.BackupScreenFeatureConfig.Companion.BACKUP_SCREEN
import javax.inject.Inject

/**
 * Configuration of the 'Jetpack Backup Screen' feature.
 */
@Feature(remoteField = BACKUP_SCREEN, defaultValue = true)
class BackupScreenFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.BACKUP_SCREEN_AVAILABLE,
        BACKUP_SCREEN
) {
    companion object {
        const val BACKUP_SCREEN = "backup_screen"
    }
}
