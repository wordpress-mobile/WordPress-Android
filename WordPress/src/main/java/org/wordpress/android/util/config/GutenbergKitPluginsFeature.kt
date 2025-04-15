package org.wordpress.android.util.config

import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.GutenbergKitPluginsFeature.Companion.GUTENBERG_KIT_PLUGINS_FIELD
import javax.inject.Inject

/**
 * Configuration for enabling/disabling Gutenberg Kit plugins
 */
@Feature(remoteField = GUTENBERG_KIT_PLUGINS_FIELD, defaultValue = false)
class GutenbergKitPluginsFeature
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    false,
    GUTENBERG_KIT_PLUGINS_FIELD
) {
    companion object {
        const val GUTENBERG_KIT_PLUGINS_FIELD = "gutenberg_kit_plugins"
    }
}
