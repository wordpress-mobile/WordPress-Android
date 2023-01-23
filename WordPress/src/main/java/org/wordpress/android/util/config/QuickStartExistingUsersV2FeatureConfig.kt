package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

/**
 * Configuration of the 'Quick Start for Existing Users V2' that will introduce a new set of
 * Quick Start steps that are relevant to existing users.
 */
@Feature(
    remoteField = QuickStartExistingUsersV2FeatureConfig.QUICK_START_EXISTING_USERS_V2,
    defaultValue = true
)
class QuickStartExistingUsersV2FeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.QUICK_START_EXISTING_USERS_V2,
    QUICK_START_EXISTING_USERS_V2
) {
    companion object {
        const val QUICK_START_EXISTING_USERS_V2 = "quick_start_existing_users_v2"
    }
}
