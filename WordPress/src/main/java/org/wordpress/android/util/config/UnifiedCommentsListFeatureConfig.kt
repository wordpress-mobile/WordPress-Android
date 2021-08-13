package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.UnifiedCommentsListFeatureConfig.Companion.UNIFIED_COMMENTS_LIST_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration of the Unified Comments list improvements
 */
@Feature(UNIFIED_COMMENTS_LIST_REMOTE_FIELD, true)
class UnifiedCommentsListFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.UNIFIED_COMMENTS_LIST,
        UNIFIED_COMMENTS_LIST_REMOTE_FIELD
) {
    companion object {
        const val UNIFIED_COMMENTS_LIST_REMOTE_FIELD = "unified_comment_list_remote_field"
    }
}
