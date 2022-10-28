package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Convenience build flag to enable/disable the Welcome Screen that follows a switch to the Jetpack app
 * This is a temporary flag that will be removed once the screen is configured to be shown at the right time.
 */
@FeatureInDevelopment
class JetpackMigrationFlowFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(appConfig, BuildConfig.JETPACK_MIGRATION_FLOW) {
    override fun isEnabled(): Boolean {
        return BuildConfig.IS_JETPACK_APP || super.isEnabled()
    }
}
