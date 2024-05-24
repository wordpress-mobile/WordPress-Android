package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val READER_ANNOUNCEMENT_CARD_REMOTE_FIELD = "reader_announcement_card"
@Feature(remoteField = READER_ANNOUNCEMENT_CARD_REMOTE_FIELD, defaultValue = true)
class ReaderAnnouncementCardFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.READER_ANNOUNCEMENT_CARD,
    READER_ANNOUNCEMENT_CARD_REMOTE_FIELD,
)
