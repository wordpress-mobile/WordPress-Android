package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.UnifiedCommentsCommentEditFeatureConfig.Companion.UNIFIED_COMMENTS_COMMENT_EDIT_FIELD
import javax.inject.Inject

@Feature(UNIFIED_COMMENTS_COMMENT_EDIT_FIELD, true)
class UnifiedCommentsCommentEditFeatureConfig @Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.UNIFIED_COMMENTS_COMMENT_EDIT,
        UNIFIED_COMMENTS_COMMENT_EDIT_FIELD
) {
    companion object {
        const val UNIFIED_COMMENTS_COMMENT_EDIT_FIELD = "unified_comments_comment_edit_enabled"
    }
}
