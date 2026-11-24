package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val READER_IN_WORDPRESS_APP_REMOTE_FIELD = "wordpress_android_reader"

/**
 * Shows the Reader in the WordPress app for users signed in to WordPress.com. Has no effect in the
 * Jetpack app, which always has the Reader. Read through [org.wordpress.android.ui.reader.ReaderAvailability].
 */
@Feature(remoteField = READER_IN_WORDPRESS_APP_REMOTE_FIELD, defaultValue = false)
class ReaderInWordPressAppFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.READER_IN_WORDPRESS_APP,
    READER_IN_WORDPRESS_APP_REMOTE_FIELD,
)
