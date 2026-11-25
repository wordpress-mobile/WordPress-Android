package org.wordpress.android.ui.reader.views.compose.article

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3WithoutBackground
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences

private const val WEIGHT_REGULAR = 400
private const val WEIGHT_MEDIUM = 500
private const val WEIGHT_SEMIBOLD = 600
private const val WEIGHT_BOLD = 700
private const val DARK_LUMINANCE = 0.5f
private const val SECONDARY_ALPHA = 0.62f
private const val BODY_ALPHA = 0.9f
private const val MUTED_ALPHA = 0.75f
private const val DIVIDER_ALPHA = 0.08f
private const val OUTLINE_ALPHA = 0.16f
private const val PILL_ALPHA = 0.94f

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(resId: Int) = FontFamily(
    listOf(WEIGHT_REGULAR, WEIGHT_MEDIUM, WEIGHT_SEMIBOLD, WEIGHT_BOLD).map { weight ->
        Font(resId, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))
    }
)

/** The interface face: titles, bylines, captions */
private val InstrumentSans = variableFamily(R.font.instrument_sans)

/** The reading face, built for legibility */
private val AtkinsonNext = variableFamily(R.font.atkinson_hyperlegible_next)

/**
 * The native post detail's colors, from WPFolio's design. The system reading theme uses WPFolio's light and dark
 * palettes; the other reading themes keep their own background and text colors.
 */
@Immutable
data class ReaderArticleColors(
    val isDark: Boolean,
    val background: Color,
    val text: Color,
    val textSecondary: Color,
    val body: Color,
    val bodyMuted: Color,
    val divider: Color,
    val outline: Color,
    val inverse: Color,
    val onInverse: Color,
    val pill: Color,
    val sheet: Color,
)

private val LightColors = ReaderArticleColors(
    isDark = false,
    background = Color(0xFFFAFAF8),
    text = Color(0xFF141414),
    textSecondary = Color(0xFF62615D),
    body = Color(0xFF2A2A28),
    bodyMuted = Color(0xFF4A4A48),
    divider = Color(0x14000000),
    outline = Color(0x24000000),
    inverse = Color(0xFF141414),
    onInverse = Color.White,
    pill = Color(0xF0161616),
    sheet = Color.White,
)

private val DarkColors = ReaderArticleColors(
    isDark = true,
    background = Color(0xFF121212),
    text = Color(0xFFF2F2F0),
    textSecondary = Color(0xFFA3A3A0),
    body = Color(0xFFD6D6D2),
    bodyMuted = Color(0xFFB9B9B5),
    divider = Color(0x12FFFFFF),
    outline = Color(0x29FFFFFF),
    inverse = Color(0xFFF2F2F0),
    onInverse = Color(0xFF141414),
    pill = Color(0xF0303032),
    sheet = Color(0xFF1F1F21),
)

private fun colorsFor(background: Color, text: Color): ReaderArticleColors {
    val isDark = background.luminance() < DARK_LUMINANCE
    return ReaderArticleColors(
        isDark = isDark,
        background = background,
        text = text,
        textSecondary = text.copy(alpha = SECONDARY_ALPHA),
        body = text.copy(alpha = BODY_ALPHA),
        bodyMuted = text.copy(alpha = MUTED_ALPHA),
        divider = text.copy(alpha = DIVIDER_ALPHA),
        outline = text.copy(alpha = OUTLINE_ALPHA),
        inverse = text,
        onInverse = background,
        pill = (if (isDark) DarkColors.pill else LightColors.pill).copy(alpha = PILL_ALPHA),
        sheet = background,
    )
}

/** The reading face and size the reader chose, applied to the post's body text */
@Immutable
data class ReaderArticleReading(val bodyFamily: FontFamily, val scale: Float)

internal val LocalReaderArticleColors = staticCompositionLocalOf { LightColors }
internal val LocalReaderArticleReading = staticCompositionLocalOf { ReaderArticleReading(AtkinsonNext, 1f) }

/** WPFolio's colors and type, following the reader's reading preferences */
@Composable
fun ReaderArticleTheme(
    readingPreferences: ReaderReadingPreferences,
    themeValues: ReaderReadingPreferences.ThemeValues,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val colors = remember(readingPreferences.theme, themeValues, isSystemDark) {
        if (readingPreferences.theme == ReaderReadingPreferences.Theme.SYSTEM) {
            if (isSystemDark) DarkColors else LightColors
        } else {
            colorsFor(Color(themeValues.intBackgroundColor), Color(themeValues.intTextColor))
        }
    }
    val reading = remember(readingPreferences.fontFamily, readingPreferences.fontSize) {
        val family = when (readingPreferences.fontFamily) {
            ReaderReadingPreferences.FontFamily.SERIF -> FontFamily.Serif
            ReaderReadingPreferences.FontFamily.MONO -> FontFamily.Monospace
            else -> AtkinsonNext
        }
        ReaderArticleReading(family, readingPreferences.fontSize.multiplier)
    }
    CompositionLocalProvider(
        LocalReaderArticleColors provides colors,
        LocalReaderArticleReading provides reading,
        content = content,
    )
}

internal val articleColors: ReaderArticleColors
    @Composable get() = LocalReaderArticleColors.current

/** Interface text in Instrument Sans, at the size of the design's CSS */
internal fun ui(size: Float, weight: Int = WEIGHT_REGULAR, lineHeight: Float? = null, tracking: Float = 0f) =
    TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight(weight),
        fontSize = size.sp,
        lineHeight = lineHeight?.em ?: TextUnit.Unspecified,
        letterSpacing = if (tracking == 0f) TextUnit.Unspecified else tracking.em,
    )

/** Reading text in the reader's face, scaled by their text size */
@Composable
internal fun body(size: Float, lineHeight: Float = 1.5f): TextStyle {
    val reading = LocalReaderArticleReading.current
    return TextStyle(
        fontFamily = reading.bodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (size * reading.scale).sp,
        lineHeight = lineHeight.em,
    )
}

/** The body size, scaled by the reader's text size, for sizes derived from it */
@Composable
internal fun bodySize(size: Float = BODY_SIZE) = size * LocalReaderArticleReading.current.scale

internal const val BODY_SIZE = 17f

/**
 * The app's M3 theme recoloured and retyped as WPFolio's, for whole screens styled like it (Reader comments):
 * WPFolio's light or dark palette, Instrument Sans for the interface, and the article locals for reading text
 */
@Composable
fun ReaderArticleMaterialTheme(content: @Composable () -> Unit) {
    AppThemeM3WithoutBackground {
        val isDark = isSystemInDarkTheme()
        val colors = if (isDark) DarkColors else LightColors
        val baseScheme = MaterialTheme.colorScheme
        val baseTypography = MaterialTheme.typography
        val scheme = remember(baseScheme, colors) {
            baseScheme.copy(
                primary = colors.text,
                onPrimary = colors.onInverse,
                background = colors.background,
                onBackground = colors.text,
                surface = colors.background,
                onSurface = colors.body,
                onSurfaceVariant = colors.textSecondary,
                surfaceContainerLowest = colors.background,
                surfaceContainerLow = colors.background,
                surfaceContainer = colors.background,
                surfaceContainerHigh = colors.sheet,
                surfaceContainerHighest = colors.divider,
                outline = colors.outline,
                outlineVariant = colors.divider,
            )
        }
        val typography = remember(baseTypography) { baseTypography.withFontFamily(InstrumentSans) }
        MaterialTheme(colorScheme = scheme, typography = typography) {
            CompositionLocalProvider(
                LocalReaderArticleColors provides colors,
                LocalContentColor provides colors.body,
                content = content,
            )
        }
    }
}

private fun Typography.withFontFamily(family: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)
