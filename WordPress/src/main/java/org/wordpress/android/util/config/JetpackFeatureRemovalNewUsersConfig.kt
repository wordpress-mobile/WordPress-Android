package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.JetpackFeatureRemovalNewUsersConfig.Companion.JETPACK_FEATURE_REMOVAL_NEW_USERS_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration for Jetpack feature removal phase new users
 */
@Feature(JETPACK_FEATURE_REMOVAL_NEW_USERS_REMOTE_FIELD, false)
class JetpackFeatureRemovalNewUsersConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.JETPACK_FEATURE_REMOVAL_NEW_USERS,
    JETPACK_FEATURE_REMOVAL_NEW_USERS_REMOTE_FIELD
) {
    companion object {
        const val JETPACK_FEATURE_REMOVAL_NEW_USERS_REMOTE_FIELD = "jp_removal_new_users"
    }
}
