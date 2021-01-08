package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig.UNREAD_POSTS_COUNT
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.UnreadPostsCountFeatureConfig.Companion.UNREAD_POSTS_COUNT_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration of the Unread Posts Count
 */
@Feature(UNREAD_POSTS_COUNT_REMOTE_FIELD, true)
class UnreadPostsCountFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        UNREAD_POSTS_COUNT,
        UNREAD_POSTS_COUNT_REMOTE_FIELD
) {
    companion object {
        const val UNREAD_POSTS_COUNT_REMOTE_FIELD = "unread_post_count"
    }
}
