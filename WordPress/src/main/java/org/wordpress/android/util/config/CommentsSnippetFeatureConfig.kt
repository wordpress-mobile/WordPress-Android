package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.CommentsSnippetFeatureConfig.Companion.COMMENTS_SNIPPET_COMMENTS_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration of the threaded comments below post improvement
 */
@Feature(COMMENTS_SNIPPET_COMMENTS_REMOTE_FIELD, true)
class CommentsSnippetFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.COMMENTS_SNIPPET,
    COMMENTS_SNIPPET_COMMENTS_REMOTE_FIELD
) {
    companion object {
        const val COMMENTS_SNIPPET_COMMENTS_REMOTE_FIELD = "comments_snippet_remote_field"
    }
}
