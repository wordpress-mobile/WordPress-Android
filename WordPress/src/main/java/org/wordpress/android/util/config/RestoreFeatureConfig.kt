package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.RestoreFeatureConfig.Companion.RESTORE_FLOW
import javax.inject.Inject

/**
 * Configuration of the 'Jetpack Restore' feature.
 */
@Feature(remoteField = RESTORE_FLOW, defaultValue = true)
class RestoreFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.RESTORE_AVAILABLE,
        RESTORE_FLOW
) {
    companion object {
        const val RESTORE_FLOW = "restore_flow"
    }
}
