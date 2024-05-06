package org.wordpress.android.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val localColors = staticCompositionLocalOf { extraPaletteLight }
internal val localTypography = staticCompositionLocalOf { extraTypography }

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
    val extraTypography = extraTypography
    CompositionLocalProvider (localColors provides extraColors, localTypography provides extraTypography) {
        MaterialTheme(
            colorScheme = if (isDarkTheme) paletteDarkScheme else paletteLightScheme,
            typography = typography,
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

private val extraTypography = ExtraTypography(
    heading1 = DesignSystemAppTypography.heading1,
    heading2 = DesignSystemAppTypography.heading2,
    heading3 = DesignSystemAppTypography.heading3,
    heading4 = DesignSystemAppTypography.heading4,
    bodyLargeEmphasized = DesignSystemAppTypography.bodyLargeEmphasized,
    bodyMediumEmphasized = DesignSystemAppTypography.bodyMediumEmphasized,
    bodySmallEmphasized = DesignSystemAppTypography.bodySmallEmphasized,
    footnote = DesignSystemAppTypography.footnote,
    footnoteEmphasized = DesignSystemAppTypography.footnoteEmphasized,
    )

data class ExtraTypography(
    val heading1: TextStyle,
    val heading2: TextStyle,
    val heading3: TextStyle,
    val heading4: TextStyle,
    val bodyLargeEmphasized: TextStyle,
    val bodyMediumEmphasized: TextStyle,
    val bodySmallEmphasized: TextStyle,
    val footnote: TextStyle,
    val footnoteEmphasized: TextStyle,
    )
@Suppress("UnusedReceiverParameter")
val Typography.heading1
    @Composable
    @ReadOnlyComposable
    get() = localTypography.current.heading1

@Suppress("UnusedReceiverParameter")
val Typography.heading2
    @Composable
    @ReadOnlyComposable
    get() = localTypography.current.heading2

@Suppress("UnusedReceiverParameter")
val Typography.heading3
    @Composable
    @ReadOnlyComposable
    get() = localTypography.current.heading3

val Typography.heading4
    @Composable
    @ReadOnlyComposable
    get() = localTypography.current.heading4

val Typography.bodyLargeEmphasized
    @Composable
    @ReadOnlyComposable
    get() = localTypography.current.bodyLargeEmphasized

val Typography.bodyMediumEmphasized
    @Composable
    @ReadOnlyComposable
    get() = localTypography.current.bodyMediumEmphasized

val Typography.bodySmallEmphasized
    @Composable
    @ReadOnlyComposable
    get() = localTypography.current.bodySmallEmphasized

val Typography.footnote
    @Composable
    @ReadOnlyComposable
    get() = localTypography.current.footnote

val Typography.footnoteEmphasized
    @Composable
    @ReadOnlyComposable
    get() = localTypography.current.footnoteEmphasized

val typography = Typography(
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp
    ),
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
