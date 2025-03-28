package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val GUTENBERG_KIT_PLUGINS_REMOTE_FIELD = "gutenberg_kit_plugins_availability"

@Feature(GUTENBERG_KIT_PLUGINS_REMOTE_FIELD, false)
class GutenbergKitPluginsFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.ENABLE_GUTENBERG_KIT_PLUGINS,
    GUTENBERG_KIT_PLUGINS_REMOTE_FIELD
)
