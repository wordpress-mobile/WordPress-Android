package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.UnifiedCommentsCommentEditFeatureConfig.Companion.UNIFIED_COMMENTS_EDIT_REMOTE_FIELD
import javax.inject.Inject

@Feature(UNIFIED_COMMENTS_EDIT_REMOTE_FIELD, true)
class UnifiedCommentsCommentEditFeatureConfig @Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    BuildConfig.UNIFIED_COMMENTS_COMMENT_EDIT,
    UNIFIED_COMMENTS_EDIT_REMOTE_FIELD
) {
    companion object {
        const val UNIFIED_COMMENTS_EDIT_REMOTE_FIELD = "unified_comments_edit_remote_field"
    }
}
