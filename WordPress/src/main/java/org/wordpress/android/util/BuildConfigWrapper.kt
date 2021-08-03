package org.wordpress.android.util

import org.wordpress.android.BuildConfig
import javax.inject.Inject

class BuildConfigWrapper @Inject constructor() {
    fun getAppVersionCode(): Int {
        return BuildConfig.VERSION_CODE
    }

    fun getAppVersionName(): String {
        return BuildConfig.VERSION_NAME
    }

    fun isDebug(): Boolean {
        return BuildConfig.DEBUG
    }

    fun isManualFeatureConfigEnabled(): Boolean = BuildConfig.ENABLE_FEATURE_CONFIGURATION

    val isJetpackApp = BuildConfig.IS_JETPACK_APP
}
