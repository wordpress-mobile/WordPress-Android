package org.wordpress.android.ui.domains.management

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
import org.wordpress.android.ui.compose.theme.AppColor


data class ExtraColors(
    val success: Color,
    val warning: Color,
)

private val extraPaletteLight = ExtraColors(
    success = AppColor.JetpackGreen50,
    warning = AppColor.Orange50,
)

private val extraPaletteDark = ExtraColors(
    success = AppColor.JetpackGreen30,
    warning = AppColor.Orange40,
)

private val localColors = staticCompositionLocalOf { extraPaletteLight }

@Composable
fun M3Theme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    M3ThemeWithoutBackground(isDarkTheme) {
        ContentInSurface(content)
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
                    error = AppColor.Red30,
                )
            } else {
                lightColorScheme(
                    error = AppColor.Red50,
                )
            },
            content = content
        )
    }
}

// Provide extra semantic colors

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
