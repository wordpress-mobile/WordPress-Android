package org.wordpress.android.util.extensions

import android.os.Build
import android.view.Window
import androidx.core.view.WindowInsetsControllerCompat
import org.wordpress.android.util.ColorUtils

/**
 * Note these are skipped on SDK 35+ because they conflict with API 15's edge-to-edge insets
 */
fun Window.setWindowStatusBarColor(color: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        setWindowBarColor(color, InsetsType.STATUS_BAR)
    }
}

fun Window.setWindowNavigationBarColor(color: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        setWindowBarColor(color, InsetsType.NAVIGATION_BAR)
    }
}

/**
 * Sets the status bar or navigation bar color
 */
@Suppress("DEPRECATION")
private fun Window.setWindowBarColor(color: Int, insetsType: InsetsType) {
    when (insetsType) {
        InsetsType.STATUS_BAR -> statusBarColor = color
        InsetsType.NAVIGATION_BAR -> navigationBarColor = color
    }
    val windowInsetsController = WindowInsetsControllerCompat(this, decorView)
    if (insetsType == InsetsType.STATUS_BAR) {
        windowInsetsController.isAppearanceLightStatusBars = ColorUtils.isColorLight(statusBarColor)
    }
    windowInsetsController.isAppearanceLightNavigationBars =
        ColorUtils.isColorLight(navigationBarColor)
}

private enum class InsetsType {
    STATUS_BAR,
    NAVIGATION_BAR
}
