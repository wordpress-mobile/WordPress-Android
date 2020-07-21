package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import javax.inject.Inject

/**
 * Configuration of the Modal Layout Picker (MLP) feature
 */
class ModalLayoutPickerFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(appConfig, BuildConfig.MODAL_LAYOUT_PICKER)
