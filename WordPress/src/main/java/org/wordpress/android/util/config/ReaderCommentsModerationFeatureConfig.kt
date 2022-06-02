package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.ReaderCommentsModerationFeatureConfig.Companion.READER_COMMENTS_MODERATION_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration of the reader comments moderation
 */
@Feature(READER_COMMENTS_MODERATION_REMOTE_FIELD, true)
class ReaderCommentsModerationFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.READER_COMMENTS_MODERATION,
        READER_COMMENTS_MODERATION_REMOTE_FIELD
) {
    companion object {
        const val READER_COMMENTS_MODERATION_REMOTE_FIELD = "reader_comments_moderation_field"
    }
}
