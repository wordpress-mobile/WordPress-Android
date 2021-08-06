package org.wordpress.android.util

import android.content.Context
import android.content.Intent
import javax.inject.Inject

class DebugUtils @Inject constructor(
    private val context: Context
) {
    fun restartApp() = with(context) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val restartIntent = Intent.makeRestartActivityTask(launchIntent?.component)
        startActivity(restartIntent)
        Runtime.getRuntime().exit(0)
    }
}
