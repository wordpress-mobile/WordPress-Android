package org.wordpress.android.util.extensions

import android.os.Build
import android.view.Window
import android.view.WindowInsets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.wordpress.android.util.ColorUtils

fun Window.setEdgeToEdgeContentDisplay(isEnabled: Boolean) {
    val decorFitsSystemWindows = !isEnabled
    WindowCompat.setDecorFitsSystemWindows(this, decorFitsSystemWindows)
}

@Suppress("DEPRECATION")
fun Window.setWindowStatusBarColor(color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        // Android 15+
        decorView.setOnApplyWindowInsetsListener { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars())
            view.setBackgroundColor(color)
            // Adjust padding to avoid overlap
            view.setPadding(0, statusBarInsets.top, 0, 0)
            insets
        }
    } else {
        // Android 14-
        statusBarColor = color
        val windowInsetsController = WindowInsetsControllerCompat(this, decorView)
        windowInsetsController.isAppearanceLightStatusBars = ColorUtils.isColorLight(statusBarColor)

        // we need to set the light navigation appearance here because, for some reason, changing the status bar also
        // changes the navigation bar appearance but this method is supposed to only change the status bar
        windowInsetsController.isAppearanceLightNavigationBars = ColorUtils.isColorLight(navigationBarColor)
    }
}

@Suppress("DEPRECATION")
fun Window.setWindowNavigationBarColor(color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        // Android 15+
        decorView.setOnApplyWindowInsetsListener { view, insets ->
            view.setBackgroundColor(color)
            // Adjust padding to avoid overlap
            val navBarInsets = insets.getInsets(WindowInsets.Type.navigationBars())
            view.setPadding(0, navBarInsets.top, 0, 0)
            insets
        }
    } else {
        // Android 14-
        val windowInsetsController = WindowInsetsControllerCompat(this, decorView)
        navigationBarColor = color
        windowInsetsController.isAppearanceLightNavigationBars = ColorUtils.isColorLight(navigationBarColor)
    }
}
