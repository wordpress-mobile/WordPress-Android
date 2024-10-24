package org.wordpress.android.ui.reader.views.compose.readingpreferences

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.utils.toComposeFontFamily

private const val BORDER_ALPHA_10 = 0.1f
private const val BORDER_ALPHA_100 = 1f

private val buttonWidth = 72.dp
private val buttonBorderWidth = 1.dp
private val buttonPadding = Margin.ExtraLarge.value
private val buttonSpacing = Margin.Medium.value
private val buttonShape = RoundedCornerShape(5.dp)

private val themeButtonPreviewBorderWidth = 1.dp
private val themeButtonPreviewSize = 48.dp

private val fontFamilyButtonPreviewSize = 32.sp

private val labelFontSize = 12.sp

@Composable
private fun ReadingPreferenceButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    verticalSpacing: Dp = buttonSpacing,
    buttonTypeContentDescription: String? = null,
    preview: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .semantics {
                role = Role.Button
                if (isSelected) selected = true
                buttonTypeContentDescription?.let { contentDescription = it }
            }
            .width(buttonWidth)
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
            .padding(vertical = buttonPadding),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        preview()

        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontSize = labelFontSize,
                color = MaterialTheme.colors.onSurface,
            )
        )
    }
}

@Composable
fun ReadingPreferencesThemeButton(
    theme: ReaderReadingPreferences.Theme,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val themeValues = ReaderReadingPreferences.ThemeValues.from(LocalContext.current, theme)

    ReadingPreferenceButton(
        label = stringResource(theme.displayNameRes),
        isSelected = isSelected,
        buttonTypeContentDescription = stringResource(R.string.reader_preferences_screen_theme_label),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .size(themeButtonPreviewSize)
                .clip(CircleShape)
                .border(
                    width = themeButtonPreviewBorderWidth,
                    shape = CircleShape,
                    color = MaterialTheme.colors.onSurface.copy(alpha = BORDER_ALPHA_10),
                )
                .rotate(-45f),
        ) {
            listOf(themeValues.intBaseTextColor, themeValues.intBackgroundColor).forEach { color ->
                Box(
                    modifier = Modifier
                        .height(themeButtonPreviewSize / 2)
                        .fillMaxWidth()
                        .background(color = Color(color)),
                )
            }
        }
    }
}

@Composable
fun ReadingPreferencesFontFamilyButton(
    fontFamily: ReaderReadingPreferences.FontFamily,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    ReadingPreferenceButton(
        label = stringResource(fontFamily.displayNameRes),
        isSelected = isSelected,
        verticalSpacing = 0.dp,
        buttonTypeContentDescription = stringResource(R.string.reader_preferences_screen_font_family_label),
        onClick = onClick,
    ) {
        Text(
            text = stringResource(R.string.reader_preferences_screen_font_family_preview),
            style = TextStyle(
                fontFamily = fontFamily.toComposeFontFamily(),
                fontSize = fontFamilyButtonPreviewSize,
                fontWeight = FontWeight.Medium,
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false
                )
            ),
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

// region Previews
@Preview
@Composable
fun ReadingPreferencesThemeButtonPreview() {
    AppThemeM2 {
        var selectedItem: ReaderReadingPreferences.Theme? by remember { mutableStateOf(null) }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Margin.Medium.value),
        ) {
            ReaderReadingPreferences.Theme.values().forEach { theme ->
                ReadingPreferencesThemeButton(
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
fun ReadingPreferencesFontFamilyButtonPreview() {
    AppThemeM2 {
        var selectedItem: ReaderReadingPreferences.FontFamily? by remember { mutableStateOf(null) }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Margin.Medium.value),
        ) {
            ReaderReadingPreferences.FontFamily.values().forEach { fontFamily ->
                ReadingPreferencesFontFamilyButton(
                    fontFamily = fontFamily,
                    isSelected = fontFamily == selectedItem,
                    onClick = { selectedItem = fontFamily }
                )
            }
        }
    }
}
// endregion
