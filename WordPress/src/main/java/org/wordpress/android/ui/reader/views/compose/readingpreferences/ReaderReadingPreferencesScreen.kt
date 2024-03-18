package org.wordpress.android.ui.reader.views.compose.readingpreferences

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences

private const val TITLE_BASE_FONT_SIZE_SP = 24
private const val TITLE_LINE_HEIGHT_MULTIPLIER = 1.2f

@Composable
fun ReaderReadingPreferencesScreen(
    currentReadingPreferences: ReaderReadingPreferences,
    onCloseClick: () -> Unit,
    onThemeClick: (ReaderReadingPreferences.Theme) -> Unit,
    onFontFamilyClick: (ReaderReadingPreferences.FontFamily) -> Unit,
    onFontSizeClick: (ReaderReadingPreferences.FontSize) -> Unit,
) {
    val themeValues = ReaderReadingPreferences.ThemeValues.from(LocalContext.current, currentReadingPreferences.theme)
    val backgroundColor = Color(themeValues.intBackgroundColor)
    val baseTextColor = Color(themeValues.intBaseTextColor)
    val textColor = Color(themeValues.intTextColor)

    val fontFamily = currentReadingPreferences.fontFamily.toComposeFontFamily()
    val fontSize = currentReadingPreferences.fontSize.toSp()
    val fontSizeMultiplier = currentReadingPreferences.fontSize.multiplier

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MainTopAppBar(
            title = null,
            navigationIcon = NavigationIcons.BackIcon,
            onNavigationIconClick = onCloseClick,
            backgroundColor = backgroundColor,
            contentColor = baseTextColor,
        )

        // Preview section
        Column(
            modifier = Modifier
                .background(backgroundColor)
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            // title
            Text(
                text = stringResource(R.string.reader_preferences_screen_preview_title),
                style = getTitleTextStyle(fontFamily, fontSizeMultiplier, baseTextColor),
            )

            // TODO thomashortadev tags

            // Content
            Text(
                text = stringResource(R.string.reader_preferences_screen_preview_text),
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Normal,
                    color = textColor,
                ),
            )
        }

        // Preferences section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(MaterialTheme.colors.surface)
                .padding(vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Spacer(modifier = Modifier.width(16.dp))

                ReaderReadingPreferences.Theme.values().forEach { theme ->
                    ReaderReadingPreferencesThemeButton(
                        theme = theme,
                        isSelected = theme == currentReadingPreferences.theme,
                        onClick = { onThemeClick(theme) },
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Spacer(modifier = Modifier.width(16.dp))

                ReaderReadingPreferences.FontFamily.values().forEach { fontFamily ->
                    ReaderReadingPreferencesFontFamilyButton(
                        fontFamily = fontFamily,
                        isSelected = fontFamily == currentReadingPreferences.fontFamily,
                        onClick = { onFontFamilyClick(fontFamily) },
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))
            }

            FontSizeSlider(
                selectedFontSize = currentReadingPreferences.fontSize,
                onFontSizeClick = onFontSizeClick,
            )
        }
    }
}

private fun getTitleTextStyle(
    fontFamily: FontFamily,
    fontSizeMultiplier: Float,
    color: Color,
): TextStyle {
    val fontSize = (TITLE_BASE_FONT_SIZE_SP * fontSizeMultiplier).toInt()

    return TextStyle(
        fontFamily = fontFamily,
        fontSize = fontSize.sp,
        lineHeight = (TITLE_LINE_HEIGHT_MULTIPLIER * fontSize).sp,
        fontWeight = FontWeight.Medium,
        color = color,
    )
}

@Composable
fun FontSizeSlider(
    selectedFontSize: ReaderReadingPreferences.FontSize,
    onFontSizeClick: (ReaderReadingPreferences.FontSize) -> Unit,
) {
    // TODO slider with previews and stops
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ReaderReadingPreferences.FontSize.values().forEach { fontSize ->
            Text(
                text = fontSize.value.toString(),
                modifier = Modifier
                    .clickable { onFontSizeClick(fontSize) }
                    .border(
                        width = 1.dp,
                        color = if (fontSize == selectedFontSize) {
                            MaterialTheme.colors.onSurface
                        } else {
                            MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
                        }
                    )
                    .padding(8.dp),
                style = TextStyle(
                    fontSize = 32.sp,
                    color = MaterialTheme.colors.onSurface,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun ReaderReadingPreferencesScreenPreview() {
    AppTheme {
        var readingPreferences by remember { mutableStateOf(ReaderReadingPreferences()) }

        ReaderReadingPreferencesScreen(
            currentReadingPreferences = readingPreferences,
            onCloseClick = {},
            onThemeClick = { readingPreferences = readingPreferences.copy(theme = it) },
            onFontFamilyClick = { readingPreferences = readingPreferences.copy(fontFamily = it) },
            onFontSizeClick = { readingPreferences = readingPreferences.copy(fontSize = it) },
        )
    }
}
