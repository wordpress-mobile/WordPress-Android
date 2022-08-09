package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.FeatureInDevelopment
import javax.inject.Inject

/**
 * Configuration for Jetpack Powered Bottom Sheet
 *
 * TODO: When it is ready to be rolled out uncomment the lines 12 and 19, remove line 13 and this to-do
 */
// @Feature(JetpackPoweredBottomSheetFeatureConfig.JETPACK_POWERED_BOTTOM_SHEET_REMOTE_FIELD, true)
@FeatureInDevelopment
@Suppress("ForbiddenComment")
class JetpackPoweredBottomSheetFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
        appConfig,
        BuildConfig.JETPACK_POWERED,
//        JETPACK_POWERED_BOTTOM_SHEET_REMOTE_FIELD
) {
    companion object {
        const val JETPACK_POWERED_BOTTOM_SHEET_REMOTE_FIELD = "jetpack_powered_bottom_sheet_remote_field"
    }
}
