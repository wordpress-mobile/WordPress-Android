package org.wordpress.android.ui.main.feedbackform

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.wordpress.android.BuildConfig
import org.wordpress.android.ui.compose.theme.AppColor

private val localColors = staticCompositionLocalOf { extraPaletteJPLight }

/**
 * This is a possible Material3 replacement for AppTheme but has not been thoroughly tested.
 * When this theme is deemed ready to use, it should be moved to the ../compose/theme package.
 * We will also likely want it to replace the M3Theme in the ../domains.management package.
 */
@Composable
fun AppThemeMaterial3(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    AppThemeMaterial3WithoutBackground(isDarkTheme) {
        ContentInSurface(content)
    }
}

@Composable
fun AppThemeMaterial3WithoutBackground(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isJetpackApp: Boolean = BuildConfig.IS_JETPACK_APP,
    content: @Composable () -> Unit
) {
    val extraColors = getExtraColors(
        isDarkTheme = isDarkTheme,
        isJetpackApp = isJetpackApp
    )
    val colorScheme = getColorScheme(
        isDarkTheme = isDarkTheme,
        isJetpackApp = isJetpackApp
    )
    CompositionLocalProvider(localColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

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

private val colorSchemeJPLight = lightColorScheme(
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

private val colorSchemeJPDark = darkColorScheme(
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

private val colorSchemeWPLight = lightColorScheme(
    primary = AppColor.Blue50,
    secondary = AppColor.Blue30,
    background = AppColor.White,
    surface = AppColor.White,
    error = AppColor.Red50,
    onPrimary = AppColor.White,
    onSecondary = AppColor.White,
    onBackground = AppColor.Black,
    onSurface = AppColor.Black,
    onError = AppColor.White
)

private val colorSchemeWPDark = darkColorScheme(
    primary = AppColor.Blue30,
    secondary = AppColor.Blue50,
    background = AppColor.DarkGray,
    surface = AppColor.DarkGray,
    error = AppColor.Red30,
    onPrimary = AppColor.Black,
    onSecondary = AppColor.White,
    onBackground = AppColor.White,
    onSurface = AppColor.White,
    onError = AppColor.Black
)

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

data class ExtraColors(
    val success: Color,
    val warning: Color,
    val neutral: Color,
    val ghost: Color,
)

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
