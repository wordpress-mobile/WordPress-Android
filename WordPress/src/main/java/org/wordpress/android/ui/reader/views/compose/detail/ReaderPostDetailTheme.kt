package org.wordpress.android.ui.reader.views.compose.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.wordpress.android.ui.compose.theme.AppThemeM3WithoutBackground
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences

private const val MEDIUM_EMPHASIS_ALPHA = 0.6f
private const val OUTLINE_ALPHA = 0.38f
private const val OUTLINE_VARIANT_ALPHA = 0.12f

/**
 * The app's M3 theme with the reading theme's background and text colors, as the `ReaderTheme.*` View styles apply
 * them to the native parts of the post detail. The system theme leaves the app's colors alone.
 */
@Composable
fun ReaderPostDetailTheme(
    readingPreferences: ReaderReadingPreferences,
    content: @Composable () -> Unit
) {
    AppThemeM3WithoutBackground {
        val theme = readingPreferences.theme
        if (theme == ReaderReadingPreferences.Theme.SYSTEM) {
            content()
        } else {
            val context = LocalContext.current
            val themeValues = remember(theme) { ReaderReadingPreferences.ThemeValues.from(context, theme) }
            val background = Color(themeValues.intBackgroundColor)
            val text = Color(themeValues.intBaseTextColor)
            val colorScheme = MaterialTheme.colorScheme.copy(
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
                outline = text.copy(alpha = OUTLINE_ALPHA),
                outlineVariant = text.copy(alpha = OUTLINE_VARIANT_ALPHA),
            )
            MaterialTheme(
                colorScheme = colorScheme,
                typography = MaterialTheme.typography,
                shapes = MaterialTheme.shapes,
                content = content
            )
        }
    }
}
