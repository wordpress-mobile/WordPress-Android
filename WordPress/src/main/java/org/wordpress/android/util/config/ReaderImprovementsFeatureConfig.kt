package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val READER_IMPROVEMENTS_REMOTE_FIELD = "reader_improvements"

@Feature(remoteField = READER_IMPROVEMENTS_REMOTE_FIELD, defaultValue = true)
class ReaderImprovementsFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.READER_IMPROVEMENTS,
    READER_IMPROVEMENTS_REMOTE_FIELD,
)
