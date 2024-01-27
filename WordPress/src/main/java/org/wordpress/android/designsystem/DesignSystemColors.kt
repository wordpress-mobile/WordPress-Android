package org.wordpress.android.designsystem

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

@SuppressLint("ConflictingOnColor")
val DesignSystemLightPalette = lightColors(
    primaryForeground = DesignSystemAppColor.Black,
    primaryBackground = DesignSystemAppColor.White,
    secondaryForeground = DesignSystemAppColor.Gray60,
    secondaryBackground = DesignSystemAppColor.OffWhite,
    tertiaryForeground = DesignSystemAppColor.Gray30,
    tertiaryBackground = DesignSystemAppColor.Gray,
    quartenaryBackground = DesignSystemAppColor.Gray2,
    brandForeground = DesignSystemAppColor.JetpackGreen50,
    brandBackground = DesignSystemAppColor.JetpackGreen50,
    error = DesignSystemAppColor.Red50,
    warning = DesignSystemAppColor.Orange40,
    wp = DesignSystemAppColor.Blue50,
    wpBackground = DesignSystemAppColor.Blue50

)

@SuppressLint("ConflictingOnColor")
val DesignSystemDarkPalette = darkColors(
    primaryForeground = DesignSystemAppColor.White,
    primaryBackground = DesignSystemAppColor.Black,
    secondaryForeground = DesignSystemAppColor.OffWhite60,
    secondaryBackground = DesignSystemAppColor.DarkGrey,
    tertiaryForeground = DesignSystemAppColor.Gray,
    tertiaryBackground = DesignSystemAppColor.Gray3,
    quartenaryBackground = DesignSystemAppColor.Gray4,
    brandForeground = DesignSystemAppColor.JetpackGreen30,
    brandBackground = DesignSystemAppColor.JetpackGreen1,
    error = DesignSystemAppColor.Red1,
    warning = DesignSystemAppColor.Orange1,
    wp = DesignSystemAppColor.Blue30,
    wpBackground = DesignSystemAppColor.Blue1
)

@Composable
fun designSystemColorPalette(isDarkTheme: Boolean = isSystemInDarkTheme()) = when (isDarkTheme) {
    true -> DesignSystemDarkPalette
    else -> DesignSystemLightPalette
}
