package org.wordpress.android.util.config

import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.GutenbergKitFeature.Companion.GUTENBERG_KIT_FIELD
import javax.inject.Inject

/**
 * Configuration for enabling/disabling Gutenberg Kit
 */
@Feature(remoteField = GUTENBERG_KIT_FIELD, defaultValue = false)
class GutenbergKitFeature
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
    appConfig,
    false,
    GUTENBERG_KIT_FIELD
) {
    companion object {
        const val GUTENBERG_KIT_FIELD = "gutenberg_kit"
    }
}
