package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.TenorFeatureConfig.Companion.TENOR_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration of the Tenor gif selection.
 */
@Feature(TENOR_REMOTE_FIELD)
class TenorFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.TENOR_AVAILABLE,
        TENOR_REMOTE_FIELD
) {
    companion object {
        const val TENOR_REMOTE_FIELD = "tenor_enabled"
    }
}
