package org.wordpress.android.designsystem

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

@SuppressLint("ConflictingOnColor")
val DesignSystemLightPalette = lightColors(
    primaryForeground = DesignSystemAppColor.Black,
    primaryBackground = DesignSystemAppColor.White,
    secondaryForeground = DesignSystemAppColor.GrayWIP2,
    secondaryBackground = DesignSystemAppColor.Gray,
    tertiaryForeground = DesignSystemAppColor.GrayWIP,
    tertiaryBackground = DesignSystemAppColor.Gray10,
    quartenaryBackground = DesignSystemAppColor.Gray30,
    brandForeground = DesignSystemAppColor.Green,
    brandBackground = DesignSystemAppColor.Green,
    error = DesignSystemAppColor.Red,
    warning = DesignSystemAppColor.Orange,
    wp = DesignSystemAppColor.Blue,
    wpBackground = DesignSystemAppColor.Blue

)

@SuppressLint("ConflictingOnColor")
val DesignSystemDarkPalette = darkColors(
    primaryForeground = DesignSystemAppColor.White,
    primaryBackground = DesignSystemAppColor.Black,
    secondaryForeground = DesignSystemAppColor.Gray20,
    secondaryBackground = DesignSystemAppColor.Gray50,
    tertiaryForeground = DesignSystemAppColor.Gray10,
    tertiaryBackground = DesignSystemAppColor.Gray60,
    quartenaryBackground = DesignSystemAppColor.Gray40,
    brandForeground = DesignSystemAppColor.Green10,
    brandBackground = DesignSystemAppColor.Green20,
    error = DesignSystemAppColor.Red10,
    warning = DesignSystemAppColor.Orange10,
    wp = DesignSystemAppColor.Blue10,
    wpBackground = DesignSystemAppColor.Blue20
)

@Composable
fun designSystemColorPalette(isDarkTheme: Boolean = isSystemInDarkTheme()) = when (isDarkTheme) {
    true -> DesignSystemDarkPalette
    else -> DesignSystemLightPalette
}
