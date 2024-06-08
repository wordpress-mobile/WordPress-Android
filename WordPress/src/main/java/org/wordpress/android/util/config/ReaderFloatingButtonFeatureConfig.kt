package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val READER_FLOATING_BUTTON_REMOTE_FIELD = "reader_floating_button"

@Feature(READER_FLOATING_BUTTON_REMOTE_FIELD, false)
class ReaderFloatingButtonFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.READER_FLOATING_BUTTON,
    READER_FLOATING_BUTTON_REMOTE_FIELD
)
