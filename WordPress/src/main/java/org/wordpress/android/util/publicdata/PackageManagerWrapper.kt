package org.wordpress.android.util.publicdata

import android.content.pm.PackageInfo
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class PackageManagerWrapper @Inject constructor(private val contextProvider: ContextProvider) {
    fun getPackageInfo(packageName: String, flags: Int = 0): PackageInfo? =
        contextProvider.getContext().packageManager.getPackageInfo(packageName, flags)
}
