package org.wordpress.android.ui.compose.theme

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
fun M3Theme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    M3ThemeWithoutBackground(isDarkTheme) {
        ContentInSurfaceM3(content)
    }
}

@Composable
fun M3ThemeWithoutBackground(
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
                    primary = AppColor.JetpackGreen30,
                    secondary = AppColor.JetpackGreen50,
                    background = AppColor.DarkGray,
                    surface = AppColor.DarkGray,
                    error = AppColor.Red30,
                    onPrimary = AppColor.Black,
                    onSecondary = AppColor.White,
                    onBackground = AppColor.White,
                    onSurface = AppColor.White,
                    onError = AppColor.Black
                )
            } else {
                lightColorScheme(
                    primary = AppColor.JetpackGreen50,
                    secondary = AppColor.JetpackGreen30,
                    background = AppColor.White,
                    surface = AppColor.White,
                    error = AppColor.Red50,
                    onPrimary = AppColor.White,
                    onSecondary = AppColor.White,
                    onBackground = AppColor.Black,
                    onSurface = AppColor.Black,
                    onError = AppColor.White
                )
            },
            content = content
        )
    }
}

// Provide extra semantic colors

private val extraPaletteLight = ExtraColors(
    success = AppColor.JetpackGreen50,
    warning = AppColor.Orange50,
    neutral = AppColor.Gray50,
    ghost = Color(0xFF2B2B55)
)

private val extraPaletteDark = ExtraColors(
    success = AppColor.JetpackGreen30,
    warning = AppColor.Orange40,
    neutral = AppColor.Gray30,
    ghost = Color.White
)

data class ExtraColors(
    val success: Color,
    val warning: Color,
    val neutral: Color,
    val ghost: Color,
)

@Suppress("UnusedReceiverParameter")
val ColorScheme.warning
    @Composable
    @ReadOnlyComposable
    get() = localColors.current.warning

@Suppress("UnusedReceiverParameter")
val ColorScheme.success
    @Composable
    @ReadOnlyComposable
    get() = localColors.current.success

@Suppress("UnusedReceiverParameter")
val ColorScheme.neutral
    @Composable
    @ReadOnlyComposable
    get() = localColors.current.neutral

@Suppress("UnusedReceiverParameter")
val ColorScheme.ghost
    @Composable
    @ReadOnlyComposable
    get() = localColors.current.ghost

@Composable
private fun ContentInSurfaceM3(
    content: @Composable () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        ProvideTextStyle(value = MaterialTheme.typography.bodyMedium) {
            content()
        }
    }
}
