package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val VOICE_TO_CONTENT_REMOTE_FIELD = "voice_to_content"

@Feature(remoteField = VOICE_TO_CONTENT_REMOTE_FIELD, defaultValue = false)
class VoiceToContentFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.VOICE_TO_CONTENT,
    VOICE_TO_CONTENT_REMOTE_FIELD,
)
