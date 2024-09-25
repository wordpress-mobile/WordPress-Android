package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

/**
 * Configuration of the self-hosted users feature
 */
private const val ENABLE_SELF_HOSTED_USERS = "enable_self_hosted_users"

@Feature(ENABLE_SELF_HOSTED_USERS, false)
class SelfHostedUsersFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    BuildConfig.ENABLE_SELF_HOSTED_USERS,
    ENABLE_SELF_HOSTED_USERS
)
