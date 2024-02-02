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
            colorScheme = if (isDarkTheme) {
                darkColorScheme(
                    primary = DesignSystemAppColor.White,
                    primaryContainer = DesignSystemAppColor.Black,
                    secondary = DesignSystemAppColor.Gray20,
                    secondaryContainer = DesignSystemAppColor.Gray70,
                    tertiary = DesignSystemAppColor.Gray10,
                    tertiaryContainer = DesignSystemAppColor.Gray80,
                    error = DesignSystemAppColor.Red10,
                )
            } else {
                lightColorScheme(
                    primary = DesignSystemAppColor.Black,
                    primaryContainer = DesignSystemAppColor.White,
                    secondary = DesignSystemAppColor.Gray40,
                    secondaryContainer = DesignSystemAppColor.Gray,
                    tertiary = DesignSystemAppColor.Gray50,
                    tertiaryContainer = DesignSystemAppColor.Gray10,
                    error = DesignSystemAppColor.Red,
                )
            },
            content = content
        )
    }
}

private val extraPaletteLight = ExtraColors(
    quartenaryContainer = DesignSystemAppColor.Gray30,
    brand = DesignSystemAppColor.Green,
    brandContainer = DesignSystemAppColor.Green,
    warning = DesignSystemAppColor.Orange,
    wp = DesignSystemAppColor.Blue,
    wpContainer = DesignSystemAppColor.Blue
    )

private val extraPaletteDark = ExtraColors(
    quartenaryContainer = DesignSystemAppColor.Gray60,
    brand = DesignSystemAppColor.Green10,
    brandContainer = DesignSystemAppColor.Green20,
    warning = DesignSystemAppColor.Orange10,
    wp = DesignSystemAppColor.Blue10,
    wpContainer = DesignSystemAppColor.Blue20
    )

data class ExtraColors(
    val quartenaryContainer: Color,
    val brand: Color,
    val brandContainer: Color,
    val warning: Color,
    val wp: Color,
    val wpContainer: Color,
    )
@Suppress("UnusedReceiverParameter")
val ColorScheme.quartenary
    @Composable
    @ReadOnlyComposable
    get() = localColors.current.quartenaryContainer

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
