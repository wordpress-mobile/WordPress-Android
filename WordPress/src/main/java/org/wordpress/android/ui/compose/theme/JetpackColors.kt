package org.wordpress.android.ui.compose.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

@SuppressLint("ConflictingOnColor")
val JpLightColorPalette = lightColors(
    primary = AppColor.JetpackGreen50,
    primaryVariant = AppColor.JetpackGreen30,
    secondary = AppColor.JetpackGreen50,
    secondaryVariant = AppColor.JetpackGreen30,
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
val JpDarkColorPalette = darkColors(
    primary = AppColor.JetpackGreen30,
    primaryVariant = AppColor.JetpackGreen50,
    secondary = AppColor.JetpackGreen30,
    secondaryVariant = AppColor.JetpackGreen50,
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
fun JpColorPalette(isDarkTheme: Boolean = isSystemInDarkTheme()) = when (isDarkTheme) {
    true -> JpDarkColorPalette
    else -> JpLightColorPalette
}
