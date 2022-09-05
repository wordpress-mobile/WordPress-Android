package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

@Feature(JetpackPoweredBottomSheetFeatureConfig.JETPACK_POWERED_BOTTOM_SHEET_REMOTE_FIELD, false)
class JetpackPoweredBottomSheetFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.JETPACK_POWERED_BOTTOM_SHEET,
        JETPACK_POWERED_BOTTOM_SHEET_REMOTE_FIELD
) {
    companion object {
        const val JETPACK_POWERED_BOTTOM_SHEET_REMOTE_FIELD = "jetpack_powered_bottom_sheet_remote_field"
    }
}
