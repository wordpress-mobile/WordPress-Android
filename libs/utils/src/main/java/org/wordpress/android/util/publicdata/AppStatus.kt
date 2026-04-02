package org.wordpress.android.util.publicdata

import android.content.pm.PackageManager.NameNotFoundException
import javax.inject.Inject

class AppStatus @Inject constructor(private val packageManagerWrapper: PackageManagerWrapper) {
    @Suppress("SwallowedException")
    fun isAppInstalled(packageName: String): Boolean =
        try {
            packageManagerWrapper.getPackageInfo(packageName)
            true
        } catch (nameNotFoundException: NameNotFoundException) {
            false
        }
}
