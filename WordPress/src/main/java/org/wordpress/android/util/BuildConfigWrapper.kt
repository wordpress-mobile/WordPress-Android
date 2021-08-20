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

    fun isDebugSettingsEnabled(): Boolean = BuildConfig.ENABLE_DEBUG_SETTINGS

    val isJetpackApp = BuildConfig.IS_JETPACK_APP
}
