package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.FollowUnfollowCommentsFeatureConfig.Companion.FOLLOW_UNFOLLOW_COMMENTS_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration of the Follow Unfollow Comments
 */
@Feature(FOLLOW_UNFOLLOW_COMMENTS_REMOTE_FIELD, true)
class FollowUnfollowCommentsFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.FOLLOW_UNFOLLOW_COMMENTS,
        FOLLOW_UNFOLLOW_COMMENTS_REMOTE_FIELD
) {
    companion object {
        const val FOLLOW_UNFOLLOW_COMMENTS_REMOTE_FIELD = "follow_unfollow_comments_enabled"
    }
}
