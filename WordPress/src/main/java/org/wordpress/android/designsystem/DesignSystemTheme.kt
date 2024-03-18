package org.wordpress.android.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val localColors = staticCompositionLocalOf { extraPaletteLight }

@Composable
fun DesignSystemTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    DesignSystemThemeWithoutBackground(isDarkTheme) {
        ContentInSurface(content)
    }
}

@Composable
fun DesignSystemThemeWithoutBackground(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val extraColors = if (isDarkTheme) {
        extraPaletteDark
    } else {
        extraPaletteLight
    }

    CompositionLocalProvider (localColors provides extraColors) {
        MaterialTheme(
            colorScheme = if (isDarkTheme) paletteDarkScheme else paletteLightScheme,
            content = content
        )
    }
}
private val paletteLightScheme = lightColorScheme(
    primary = DesignSystemAppColor.Black,
    primaryContainer = DesignSystemAppColor.White,
    secondary = DesignSystemAppColor.DarkGray55,
    secondaryContainer = DesignSystemAppColor.DarkGray8,
    tertiary = DesignSystemAppColor.DarkGray30,
    tertiaryContainer = DesignSystemAppColor.DarkGray15,
    error = DesignSystemAppColor.Red,
    )

private val paletteDarkScheme = darkColorScheme(
    primary = DesignSystemAppColor.White,
    primaryContainer = DesignSystemAppColor.Black,
    secondary = DesignSystemAppColor.Gray60,
    secondaryContainer = DesignSystemAppColor.Gray22,
    tertiary = DesignSystemAppColor.Gray,
    tertiaryContainer = DesignSystemAppColor.Gray30,
    error = DesignSystemAppColor.Red10,
    )

private val extraPaletteLight = ExtraColors(
    quaternaryContainer = DesignSystemAppColor.DarkGray40,
    brand = DesignSystemAppColor.Green,
    brandContainer = DesignSystemAppColor.Green,
    warning = DesignSystemAppColor.Orange,
    wp = DesignSystemAppColor.Blue,
    wpContainer = DesignSystemAppColor.Blue
    )

private val extraPaletteDark = ExtraColors(
    quaternaryContainer = DesignSystemAppColor.Gray40,
    brand = DesignSystemAppColor.Green10,
    brandContainer = DesignSystemAppColor.Green20,
    warning = DesignSystemAppColor.Orange10,
    wp = DesignSystemAppColor.Blue10,
    wpContainer = DesignSystemAppColor.Blue20
    )

data class ExtraColors(
    val quaternaryContainer: Color,
    val brand: Color,
    val brandContainer: Color,
    val warning: Color,
    val wp: Color,
    val wpContainer: Color,
    )
@Suppress("UnusedReceiverParameter")
val ColorScheme.quaternaryContainer
    @Composable
    @ReadOnlyComposable
    get() = localColors.current.quaternaryContainer

@Suppress("UnusedReceiverParameter")
val ColorScheme.brand
    @Composable
    @ReadOnlyComposable
    get() = localColors.current.brand

@Suppress("UnusedReceiverParameter")
val ColorScheme.brandContainer
    @Composable
    @ReadOnlyComposable
    get() = localColors.current.brandContainer

@Suppress("UnusedReceiverParameter")
val ColorScheme.warning
    @Composable
    @ReadOnlyComposable
    get() = localColors.current.warning

@Suppress("UnusedReceiverParameter")
val ColorScheme.wp
    @Composable
    @ReadOnlyComposable
    get() = localColors.current.wp

@Suppress("UnusedReceiverParameter")
val ColorScheme.wpContainer
    @Composable
    @ReadOnlyComposable
    get() = localColors.current.wpContainer

@Composable
private fun ContentInSurface(
    content: @Composable () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        ProvideTextStyle(value = MaterialTheme.typography.bodyMedium) {
            content()
        }
    }
}
