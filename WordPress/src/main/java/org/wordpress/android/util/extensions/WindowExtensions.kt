package org.wordpress.android.util.extensions

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import org.wordpress.android.R

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
    if (isLightTheme() && VERSION.SDK_INT >= VERSION_CODES.O) {
        decorView.systemUiVisibility = decorView.systemUiVisibility.let {
            if (showInLightMode) {
                it or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                it and SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
        }
        if (applyDefaultColors) {
            navigationBarColor = if (showInLightMode) {
                context.getColorFromAttribute(R.attr.colorSurface)
            } else {
                ContextCompat.getColor(context, android.R.color.black)
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

private fun Window.isLightTheme() = !context.resources.configuration.isDarkTheme()
