package org.wordpress.android.util.publicdata

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class PackageManagerWrapper @Inject constructor(private val contextProvider: ContextProvider) {
    fun getPackageInfo(packageName: String, flags: Int = 0): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            contextProvider.getContext().packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(flags.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            contextProvider.getContext().packageManager.getPackageInfo(packageName, flags)
        }
}
