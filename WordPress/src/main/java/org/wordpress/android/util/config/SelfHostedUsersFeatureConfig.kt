package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.SelfHostedUsersFeatureConfig.Companion.SELF_HOSTED_USERS_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration of the self-hosted users feature
 */
@Feature(SELF_HOSTED_USERS_REMOTE_FIELD, false)
class SelfHostedUsersFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.ENABLE_SELF_HOSTED_USERS,
    SELF_HOSTED_USERS_REMOTE_FIELD
) {
    companion object {
        const val SELF_HOSTED_USERS_REMOTE_FIELD = "self_hosted_users_remote_field"
    }
}
