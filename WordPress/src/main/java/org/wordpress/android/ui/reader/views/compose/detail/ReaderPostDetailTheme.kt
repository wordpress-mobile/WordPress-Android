package org.wordpress.android.ui.reader.views.compose.detail

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.wordpress.android.ui.compose.theme.AppThemeM3WithoutBackground
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences

internal const val HIGH_EMPHASIS_ALPHA = 0.87f
internal const val MEDIUM_EMPHASIS_ALPHA = 0.6f
internal const val DISABLED_ALPHA = 0.38f
private const val OUTLINE_VARIANT_ALPHA = 0.12f

/**
 * The reading theme's text color, which the post's title and tags take as the WebView body does
 */
internal val LocalReadingTextColor = staticCompositionLocalOf { Color.Unspecified }

/**
 * The app's M3 theme with the reading theme's background and text colors, as the `ReaderTheme.*` View styles apply
 * them to the native parts of the post detail. The system theme leaves the app's colors alone.
 *
 * [themeValues] are resolved by the caller, once for all of a screen's compositions.
 */
@Composable
fun ReaderPostDetailTheme(
    theme: ReaderReadingPreferences.Theme,
    themeValues: ReaderReadingPreferences.ThemeValues,
    content: @Composable () -> Unit
) {
    AppThemeM3WithoutBackground {
        val baseScheme = MaterialTheme.colorScheme
        val colorScheme = remember(themeValues, baseScheme) {
            if (theme == ReaderReadingPreferences.Theme.SYSTEM) {
                baseScheme
            } else {
                val background = Color(themeValues.intBackgroundColor)
                val text = Color(themeValues.intBaseTextColor)
                baseScheme.copy(
                    background = background,
                    onBackground = text,
                    surface = background,
                    onSurface = text,
                    surfaceVariant = background,
                    onSurfaceVariant = text.copy(alpha = MEDIUM_EMPHASIS_ALPHA),
                    surfaceContainerLowest = background,
                    surfaceContainerLow = background,
                    surfaceContainer = background,
                    surfaceContainerHigh = background,
                    surfaceContainerHighest = background,
                    inverseSurface = text,
                    inverseOnSurface = background,
                    inversePrimary = background,
                    outline = text.copy(alpha = DISABLED_ALPHA),
                    outlineVariant = text.copy(alpha = OUTLINE_VARIANT_ALPHA),
                )
            }
        }
        MaterialTheme(colorScheme = colorScheme) {
            // there's no Surface to set the content color, so text without a color of its own would be black,
            // unreadable on a dark theme
            CompositionLocalProvider(
                LocalContentColor provides colorScheme.onSurface,
                LocalReadingTextColor provides Color(themeValues.intTextColor),
                content = content,
            )
        }
    }
}
