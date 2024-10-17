package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val NEW_GUTENBERG_FEATURE_REMOTE_FIELD = "experimental_block_editor"

@Feature(NEW_GUTENBERG_FEATURE_REMOTE_FIELD, false)
class NewGutenbergFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.ENABLE_NEW_GUTENBERG,
    NEW_GUTENBERG_FEATURE_REMOTE_FIELD
)
