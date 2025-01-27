package org.wordpress.android.util.extensions

import android.os.Build
import android.view.Window
import android.view.WindowInsets
import androidx.core.view.WindowInsetsControllerCompat
import org.wordpress.android.util.ColorUtils

fun Window.setWindowStatusBarColor(color: Int) {
    setWindowBarColor(color, InsetsType.STATUS_BAR)
}

fun Window.setWindowNavigationBarColor(color: Int) {
    setWindowBarColor(color, InsetsType.NAVIGATION_BAR)
}

/**
 * Sets the status bar or navigation bar color
 * TODO Setting both the status bar color and navigation bar color causes the insets to be set twice
 */
@Suppress("DEPRECATION")
private fun Window.setWindowBarColor(color: Int, insetsType: InsetsType) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        // Android 15+
        decorView.setOnApplyWindowInsetsListener { view, insets ->
            view.setBackgroundColor(color)
            val barInsets = insets.getInsets(
                when (insetsType) {
                    InsetsType.STATUS_BAR -> WindowInsets.Type.statusBars()
                    InsetsType.NAVIGATION_BAR -> WindowInsets.Type.navigationBars()
                }
            )
            // Adjust padding to avoid overlap
            view.setPadding(0, barInsets.top, 0, 0)
            insets
        }
    } else {
        when(insetsType) {
            InsetsType.STATUS_BAR -> statusBarColor = color
            InsetsType.NAVIGATION_BAR -> navigationBarColor = color
        }
        val windowInsetsController = WindowInsetsControllerCompat(this, decorView)
        if (insetsType == InsetsType.STATUS_BAR) {
            windowInsetsController.isAppearanceLightStatusBars = ColorUtils.isColorLight(statusBarColor)
        }
        windowInsetsController.isAppearanceLightNavigationBars = ColorUtils.isColorLight(navigationBarColor)
    }
}

private enum class InsetsType {
    STATUS_BAR,
    NAVIGATION_BAR
}
