package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.UnifiedThreadedCommentsFeatureConfig.Companion.UNIFIED_THREADED_COMMENTS_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration of the threaded comments below post improvement
 */
@Feature(UNIFIED_THREADED_COMMENTS_REMOTE_FIELD, false)
class UnifiedThreadedCommentsFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.UNIFIED_THREADED_COMMENTS,
        UNIFIED_THREADED_COMMENTS_REMOTE_FIELD
) {
    companion object {
        const val UNIFIED_THREADED_COMMENTS_REMOTE_FIELD = "unified_threaded_comments_remote_field"
    }
}
