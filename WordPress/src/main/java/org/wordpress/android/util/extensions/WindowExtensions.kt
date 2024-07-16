package org.wordpress.android.util.extensions

import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.wordpress.android.util.ColorUtils
import android.R as AndroidR
import com.google.android.material.R as MaterialR

@Suppress("DEPRECATION")
fun Window.setLightStatusBar(showInLightMode: Boolean) {
    if (isLightTheme()) {
        decorView.systemUiVisibility = decorView.systemUiVisibility.let {
            if (showInLightMode) {
                it or SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                it and SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }
}

@Suppress("DEPRECATION")
fun Window.setLightNavigationBar(showInLightMode: Boolean, applyDefaultColors: Boolean = false) {
    if (isLightTheme()) {
        decorView.systemUiVisibility = decorView.systemUiVisibility.let {
            if (showInLightMode) {
                it or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                it and SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
        }
        if (applyDefaultColors) {
            navigationBarColor = if (showInLightMode) {
                context.getColorFromAttribute(MaterialR.attr.colorSurface)
            } else {
                ContextCompat.getColor(context, AndroidR.color.black)
            }
        }
    }
}

fun Window.setEdgeToEdgeContentDisplay(isEnabled: Boolean) {
    val decorFitsSystemWindows = !isEnabled
    WindowCompat.setDecorFitsSystemWindows(this, decorFitsSystemWindows)
}

@Suppress("DEPRECATION")
fun Window.showFullScreen() {
    decorView.systemUiVisibility = decorView.systemUiVisibility.let {
        it or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}

fun Window.setWindowStatusBarColor(color: Int) {
    val windowInsetsController = WindowInsetsControllerCompat(this, decorView)

    statusBarColor = color
    windowInsetsController.isAppearanceLightStatusBars = ColorUtils.isColorLight(statusBarColor)

    // we need to set the light navigation appearance here because, for some reason, changing the status bar also
    // changes the navigation bar appearance but this method is supposed to only change the status bar
    windowInsetsController.isAppearanceLightNavigationBars = ColorUtils.isColorLight(navigationBarColor)
}

fun Window.setWindowNavigationBarColor(color: Int) {
    val windowInsetsController = WindowInsetsControllerCompat(this, decorView)

    navigationBarColor = color
    windowInsetsController.isAppearanceLightNavigationBars = ColorUtils.isColorLight(navigationBarColor)
}

private fun Window.isLightTheme() = !context.resources.configuration.isDarkTheme()
