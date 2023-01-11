package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

@Feature(JetpackReaderSavedPostsFeatureConfig.JETPACK_READER_SAVED_POSTS_REMOTE_FIELD, false)
class JetpackReaderSavedPostsFeatureConfig
@Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.JETPACK_READER_SAVED_POSTS,
    JETPACK_READER_SAVED_POSTS_REMOTE_FIELD
) {
    companion object {
        const val JETPACK_READER_SAVED_POSTS_REMOTE_FIELD = "jetpack_reader_saved_posts_remote_field"
    }
}
