package org.wordpress.android.util

import javax.inject.Inject

class VersionCodeProvider @Inject constructor() {
    fun getVersionCode(): Int {
        return BuildConfig.VERSION_CODE
    }
}
