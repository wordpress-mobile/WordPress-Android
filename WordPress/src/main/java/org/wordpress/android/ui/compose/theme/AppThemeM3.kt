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
import org.wordpress.android.BuildConfig

private val localColors = staticCompositionLocalOf { extraPaletteJPLight }

@Composable
fun AppThemeM3(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isJetpackApp: Boolean = BuildConfig.IS_JETPACK_APP,
    content: @Composable () -> Unit
) {
    AppThemeM3WithoutBackground(isDarkTheme, isJetpackApp) {
        ContentInSurfaceM3(content)
    }
}

@Composable
fun AppThemeM3WithoutBackground(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isJetpackApp: Boolean = BuildConfig.IS_JETPACK_APP,
    content: @Composable () -> Unit
) {
    val extraColors = getExtraColors(
        isDarkTheme = isDarkTheme,
        isJetpackApp = isJetpackApp
    )
    CompositionLocalProvider(localColors provides extraColors) {
        MaterialTheme(
            colorScheme = getColorScheme(
                isDarkTheme = isDarkTheme,
                isJetpackApp = isJetpackApp
            ),
            content = content
        )
    }
}

/**
 * This theme should *only* be used in the context of the Editor (e.g. Post Settings).
 * More info: https://github.com/wordpress-mobile/gutenberg-mobile/issues/4889
 */
@Composable
fun AppThemeM3Editor(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isJetpackApp: Boolean = BuildConfig.IS_JETPACK_APP,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = getColorScheme(isDarkTheme = isDarkTheme, isJetpackApp = isJetpackApp),
        content = content
    )
}


// Provide color schemes

@Suppress("SameParameterValue")
private fun getColorScheme(
    isDarkTheme: Boolean,
    isJetpackApp: Boolean
): ColorScheme {
    return if (isJetpackApp) {
        if (isDarkTheme) {
            colorSchemeJPDark
        } else {
            colorSchemeJPLight
        }
    } else if (isDarkTheme) {
        colorSchemeWPDark
    } else {
        colorSchemeWPLight
    }
}

// Any role left unset here falls back to the M3 baseline palette, which is purple-tinted. The surface, outline
// and inverse roles carry no brand hue, so they're shared by both flavors and derived from the Automattic Color
// Studio grays; each scheme below then overrides only the roles that are actually brand-specific.
private val baseLightScheme = lightColorScheme(
    background = AppColor.White,
    onBackground = AppColor.Black,
    surface = AppColor.White,
    onSurface = AppColor.Black,
    surfaceBright = AppColor.White,
    surfaceDim = AppColor.Gray5,
    surfaceContainerLowest = AppColor.White,
    surfaceContainerLow = AppColor.Gray0,
    surfaceContainer = AppColor.Gray2,
    surfaceContainerHigh = AppColor.Gray3,
    surfaceContainerHighest = AppColor.Gray5,
    surfaceVariant = AppColor.Gray5,
    onSurfaceVariant = AppColor.Gray60,
    outline = AppColor.Gray40,
    outlineVariant = AppColor.Gray10,
    inverseSurface = AppColor.Gray90,
    inverseOnSurface = AppColor.Gray0,
    // Matching surface cancels the tonal elevation blend, so elevated surfaces stay neutral and rely on their
    // shadow instead of picking up a brand-colored wash.
    surfaceTint = AppColor.White,
    onPrimary = AppColor.White,
    onSecondary = AppColor.White,
    error = AppColor.Red50,
    onError = AppColor.White
)

private val baseDarkScheme = darkColorScheme(
    background = AppColor.DarkGray,
    onBackground = AppColor.White,
    surface = AppColor.DarkGray,
    onSurface = AppColor.White,
    surfaceBright = AppColor.Gray70,
    surfaceDim = AppColor.Gray100,
    surfaceContainerLowest = AppColor.Gray100,
    surfaceContainerLow = AppColor.Gray95,
    surfaceContainer = AppColor.Gray90,
    surfaceContainerHigh = AppColor.Gray85,
    surfaceContainerHighest = AppColor.Gray80,
    surfaceVariant = AppColor.Gray70,
    onSurfaceVariant = AppColor.Gray10,
    outline = AppColor.Gray30,
    outlineVariant = AppColor.Gray70,
    inverseSurface = AppColor.Gray5,
    inverseOnSurface = AppColor.Gray90,
    surfaceTint = AppColor.DarkGray,
    onPrimary = AppColor.Black,
    onSecondary = AppColor.White,
    error = AppColor.Red30,
    onError = AppColor.Black
)

// inversePrimary is the snackbar action label, painted on the light inverseSurface, so in dark mode it takes the
// 70 tone rather than the scheme's own primary, which would only reach ~3.4:1 there.
private val colorSchemeJPLight = baseLightScheme.copy(
    primary = AppColor.JetpackGreen50,
    secondary = AppColor.JetpackGreen30,
    primaryContainer = AppColor.JetpackGreen5,
    onPrimaryContainer = AppColor.JetpackGreen90,
    secondaryContainer = AppColor.JetpackGreen5,
    onSecondaryContainer = AppColor.JetpackGreen90,
    inversePrimary = AppColor.JetpackGreen30
)

private val colorSchemeJPDark = baseDarkScheme.copy(
    primary = AppColor.JetpackGreen30,
    secondary = AppColor.JetpackGreen50,
    primaryContainer = AppColor.JetpackGreen70,
    onPrimaryContainer = AppColor.JetpackGreen5,
    secondaryContainer = AppColor.JetpackGreen70,
    onSecondaryContainer = AppColor.JetpackGreen5,
    inversePrimary = AppColor.JetpackGreen70
)

private val colorSchemeWPLight = baseLightScheme.copy(
    primary = AppColor.Blue50,
    secondary = AppColor.Blue30,
    primaryContainer = AppColor.Blue5,
    onPrimaryContainer = AppColor.Blue80,
    secondaryContainer = AppColor.Blue5,
    onSecondaryContainer = AppColor.Blue80,
    inversePrimary = AppColor.Blue30
)

private val colorSchemeWPDark = baseDarkScheme.copy(
    primary = AppColor.Blue30,
    secondary = AppColor.Blue50,
    primaryContainer = AppColor.Blue70,
    onPrimaryContainer = AppColor.Blue5,
    secondaryContainer = AppColor.Blue70,
    onSecondaryContainer = AppColor.Blue5,
    inversePrimary = AppColor.Blue70
)

// Provide extra semantic colors

@Suppress("SameParameterValue")
private fun getExtraColors(
    isDarkTheme: Boolean,
    isJetpackApp: Boolean
): ExtraColors {
    return if (isJetpackApp) {
        if (isDarkTheme) {
            extraPaletteJPDark
        } else {
            extraPaletteJPLight
        }
    } else if (isDarkTheme) {
        extraPaletteWPDark
    } else {
        extraPaletteWPLight
    }
}

private val extraPaletteJPLight = ExtraColors(
    success = AppColor.JetpackGreen50,
    warning = AppColor.Orange50,
    neutral = AppColor.Gray50,
    ghost = Color(0xFF2B2B55)
)

private val extraPaletteJPDark = ExtraColors(
    success = AppColor.JetpackGreen30,
    warning = AppColor.Orange40,
    neutral = AppColor.Gray30,
    ghost = Color.White
)

private val extraPaletteWPLight = ExtraColors(
    success = AppColor.Blue50,
    warning = AppColor.Orange50,
    neutral = AppColor.Gray50,
    ghost = Color(0xFF2B2B55)
)

private val extraPaletteWPDark = ExtraColors(
    success = AppColor.Blue30,
    warning = AppColor.Orange40,
    neutral = AppColor.Gray30,
    ghost = Color.White
)

private data class ExtraColors(
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
