package org.wordpress.android.ui.reader.views.compose.readingpreferences

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences

private const val BORDER_ALPHA_10 = 0.1f
private const val BORDER_ALPHA_100 = 1f

private val buttonBorderWidth = 1.dp
private val buttonPadding = 16.dp
private val buttonSpacing = 8.dp
private val buttonShape = RoundedCornerShape(5.dp)

private val themeButtonPreviewBorderWidth = 1.dp
private val themeButtonPreviewSize = 48.dp

private val fontFamilyButtonPreviewSize = 32.sp

@Composable
private fun ReadingPreferenceButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colors.surface,
                shape = buttonShape,
            )
            .border(
                width = buttonBorderWidth,
                shape = buttonShape,
                color = if (isSelected) {
                    MaterialTheme.colors.onSurface.copy(alpha = BORDER_ALPHA_100)
                } else {
                    MaterialTheme.colors.onSurface.copy(alpha = BORDER_ALPHA_10)
                },
            )
            .clickable { onClick() }
            .padding(buttonPadding),
        verticalArrangement = Arrangement.spacedBy(buttonSpacing, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
fun ReaderReadingPreferencesThemeButton(
    theme: ReaderReadingPreferences.Theme,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val themeValues = ReaderReadingPreferences.ThemeValues.from(LocalContext.current, theme)

    ReadingPreferenceButton(
        isSelected = isSelected,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .size(themeButtonPreviewSize)
                .background(
                    color = Color(themeValues.intBackgroundColor),
                    shape = CircleShape,
                )
                .border(
                    width = themeButtonPreviewBorderWidth,
                    shape = CircleShape,
                    color = MaterialTheme.colors.onSurface.copy(alpha = BORDER_ALPHA_10),
                ),
        )

        Text(text = stringResource(theme.displayNameRes))
    }
}

@Composable
fun ReaderReadingPreferencesFontFamilyButton(
    fontFamily: ReaderReadingPreferences.FontFamily,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    ReadingPreferenceButton(
        isSelected = isSelected,
        onClick = onClick
    ) {
        Text(
            text = stringResource(R.string.reader_preferences_font_family_preview),
            style = TextStyle(
                fontFamily = fontFamily.toComposeFontFamily(),
                fontSize = fontFamilyButtonPreviewSize,
                fontWeight = FontWeight.Medium,
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false
                )
            )
        )

        Text(text = stringResource(fontFamily.displayNameRes))
    }
}

// region Previews
@Preview
@Composable
fun ReaderReadingPreferencesThemeButtonPreview() {
    AppTheme {
        var selectedItem: ReaderReadingPreferences.Theme? by remember { mutableStateOf(null) }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReaderReadingPreferences.Theme.values().forEach { theme ->
                ReaderReadingPreferencesThemeButton(
                    theme = theme,
                    isSelected = theme == selectedItem,
                    onClick = { selectedItem = theme }
                )
            }
        }
    }
}

@Preview
@Composable
fun ReaderReadingPreferencesFontFamilyButtonPreview() {
    AppTheme {
        var selectedItem: ReaderReadingPreferences.FontFamily? by remember { mutableStateOf(null) }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReaderReadingPreferences.FontFamily.values().forEach { fontFamily ->
                ReaderReadingPreferencesFontFamilyButton(
                    fontFamily = fontFamily,
                    isSelected = fontFamily == selectedItem,
                    onClick = { selectedItem = fontFamily }
                )
            }
        }
    }
}
// endregion
