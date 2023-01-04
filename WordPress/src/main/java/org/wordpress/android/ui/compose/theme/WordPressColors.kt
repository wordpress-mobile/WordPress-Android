package org.wordpress.android.ui.compose.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

@SuppressLint("ConflictingOnColor")
val WpLightColorPalette = lightColors(
        primary = AppColor.Blue50,
        primaryVariant = AppColor.Blue70,
        secondary = AppColor.Blue50,
        secondaryVariant = AppColor.Blue70,
        background = AppColor.White,
        surface = AppColor.White,
        error = AppColor.Red50,
        onPrimary = AppColor.White,
        onSecondary = AppColor.White,
        onBackground = AppColor.Black,
        onSurface = AppColor.Black,
        onError = AppColor.White
)

@SuppressLint("ConflictingOnColor")
val WpDarkColorPalette = darkColors(
        primary = AppColor.Blue30,
        primaryVariant = AppColor.Blue50,
        secondary = AppColor.Blue30,
        secondaryVariant = AppColor.Blue50,
        background = AppColor.DarkGray,
        surface = AppColor.DarkGray,
        error = AppColor.Red30,
        onPrimary = AppColor.Black,
        onSecondary = AppColor.White,
        onBackground = AppColor.White,
        onSurface = AppColor.White,
        onError = AppColor.Black
)

@Composable
fun WpColorPalette(isDarkTheme: Boolean = isSystemInDarkTheme()) = when (isDarkTheme) {
    true -> WpDarkColorPalette
    else -> WpLightColorPalette
}
