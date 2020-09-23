package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration of the Modal Layout Picker (MLP) feature
 */
@FeatureInDevelopment
class ModalLayoutPickerFeatureConfig
@Inject constructor(appConfig: AppConfig) : FeatureConfig(appConfig, BuildConfig.MODAL_LAYOUT_PICKER)
