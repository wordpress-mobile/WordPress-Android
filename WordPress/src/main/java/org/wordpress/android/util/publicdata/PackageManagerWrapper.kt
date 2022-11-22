package org.wordpress.android.util.publicdata

import android.content.ContextWrapper
import android.content.pm.PackageInfo
import javax.inject.Inject

class PackageManagerWrapper @Inject constructor(private val contextWrapper: ContextWrapper) {
    fun getPackageInfo(packageName: String, flags: Int = 0): PackageInfo? =
            contextWrapper.packageManager.getPackageInfo(packageName, flags)
}
