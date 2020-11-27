package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the Follow Unfollow Comments
 */
@FeatureInDevelopment
class FollowUnfollowCommentsFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.FOLLOW_UNFOLLOW_COMMENTS
)
