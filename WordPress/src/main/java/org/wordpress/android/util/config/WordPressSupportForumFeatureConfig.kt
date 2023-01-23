package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.WordPressSupportForumFeatureConfig.Companion.WORDPRESS_SUPPORT_FORUM_REMOTE_FIELD
import javax.inject.Inject

@Feature(WORDPRESS_SUPPORT_FORUM_REMOTE_FIELD, false)
class WordPressSupportForumFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.ENABLE_WORDPRESS_SUPPORT_FORUM,
    WORDPRESS_SUPPORT_FORUM_REMOTE_FIELD
) {
    companion object {
        const val WORDPRESS_SUPPORT_FORUM_REMOTE_FIELD = "wordpress_support_forum_remote_field"
    }
}
