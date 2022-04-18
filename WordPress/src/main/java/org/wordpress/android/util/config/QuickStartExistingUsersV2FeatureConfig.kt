package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the 'Quick Start for Existing Users V2' that will introduce a new set of
 * Quick Start steps that are relevant to existing users.
 */
@FeatureInDevelopment
class QuickStartExistingUsersV2FeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.QUICK_START_EXISTING_USERS_V2
)
