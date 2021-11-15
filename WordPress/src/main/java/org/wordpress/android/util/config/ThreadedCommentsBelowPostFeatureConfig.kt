package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.ThreadedCommentsBelowPostFeatureConfig.Companion.THREADED_COMMENTS_BELOW_POST_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration of the threaded comments below post improvement
 */
@Feature(THREADED_COMMENTS_BELOW_POST_REMOTE_FIELD, false)
class ThreadedCommentsBelowPostFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.THREADED_COMMENTS_BELOW_POST,
        THREADED_COMMENTS_BELOW_POST_REMOTE_FIELD
) {
    companion object {
        const val THREADED_COMMENTS_BELOW_POST_REMOTE_FIELD = "threaded_comments_below_post_remote_field"
    }
}
