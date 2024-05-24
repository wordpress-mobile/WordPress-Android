package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val READER_TAGS_FEED_REMOTE_FIELD = "reader_tags_feed"

@Feature(remoteField = READER_TAGS_FEED_REMOTE_FIELD, defaultValue = true)
class ReaderTagsFeedFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.READER_TAGS_FEED,
    READER_TAGS_FEED_REMOTE_FIELD,
)
