package org.wordpress.android.util

import org.wordpress.android.BuildConfig
import javax.inject.Inject

class BuildConfigWrapper @Inject constructor() {
    fun getAppVersionCode(): Int {
        return BuildConfig.VERSION_CODE
    }
}
