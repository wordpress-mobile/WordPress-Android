package org.wordpress.android.util.crashlogging

import android.content.pm.PackageManager.NameNotFoundException
import org.wordpress.android.util.publicdata.PackageManagerWrapper
import javax.inject.Inject

class WebviewVersionProvider @Inject constructor(private val packageManager: PackageManagerWrapper) {
    private val webviewPackageName = "com.google.android.webview"
    private val unknownVersion = "unknown"

    @Suppress("SwallowedException")
    fun getVersion(): String = try {
        packageManager.getPackageInfo(webviewPackageName, 0)?.versionName ?: unknownVersion
    } catch (e: NameNotFoundException) {
        unknownVersion
    }
}
