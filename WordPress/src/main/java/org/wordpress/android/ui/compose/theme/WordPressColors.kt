package org.wordpress.android.ui.compose.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val blue30 = Color(0xff399ce3)
private val blue50 = Color(0xff0675c4)
private val blue70 = Color(0xff044b7a)
private val red50 = Color(0xffd63638)
private val red30 = Color(0xfff86368)
private val white = Color(0xffffffff)
private val black = Color(0xff000000)
private val darkGray = Color(0xff121212)

@SuppressLint("ConflictingOnColor")
val WpLightColorPalette = lightColors(
        primary = blue50,
        primaryVariant = blue70,
        secondary = blue50,
        secondaryVariant = blue70,
        background = white,
        surface = white,
        error = red50,
        onPrimary = white,
        onSecondary = white,
        onBackground = black,
        onSurface = black,
        onError = white
)

@SuppressLint("ConflictingOnColor")
val WpDarkColorPalette = darkColors(
        primary = blue30,
        primaryVariant = blue50,
        secondary = blue30,
        secondaryVariant = blue50,
        background = darkGray,
        surface = darkGray,
        error = red30,
        onPrimary = black,
        onSecondary = white,
        onBackground = white,
        onSurface = white,
        onError = black
)

@Composable
fun WpColorPalette(isDarkTheme: Boolean = isSystemInDarkTheme()) = when (isDarkTheme) {
    true -> WpDarkColorPalette
    else -> WpLightColorPalette
}
