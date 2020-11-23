package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import org.wordpress.android.util.config.ConsolidatedMediaPickerFeatureConfig.Companion.CONSOLIDATED_MEDIA_PICKER_REMOTE_FIELD
import javax.inject.Inject

/**
 * Configuration of the Consolidated media picker
 */
@Feature(CONSOLIDATED_MEDIA_PICKER_REMOTE_FIELD, true)
class ConsolidatedMediaPickerFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(
        appConfig,
        BuildConfig.CONSOLIDATED_MEDIA_PICKER,
        CONSOLIDATED_MEDIA_PICKER_REMOTE_FIELD
) {
    companion object {
        const val CONSOLIDATED_MEDIA_PICKER_REMOTE_FIELD = "consolidated_media_picker_enabled"
    }
}
