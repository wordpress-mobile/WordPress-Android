package org.wordpress.android.ui.reader.views.compose.readingpreferences

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.utils.toComposeFontFamily
import org.wordpress.android.ui.reader.utils.toSp

private const val TITLE_BASE_FONT_SIZE_SP = 24
private const val TITLE_LINE_HEIGHT_MULTIPLIER = 1.2f
private const val TEXT_LINE_HEIGHT_MULTIPLIER = 1.6f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingPreferencesScreen(
    currentReadingPreferences: ReaderReadingPreferences,
    showSaveAction: Boolean,
    onCloseClick: () -> Unit,
    onSaveClick: () -> Unit,
    onThemeClick: (ReaderReadingPreferences.Theme) -> Unit,
    onFontFamilyClick: (ReaderReadingPreferences.FontFamily) -> Unit,
    onFontSizeClick: (ReaderReadingPreferences.FontSize) -> Unit,
) {
    val themeValues = ReaderReadingPreferences.ThemeValues.from(
        LocalContext.current,
        currentReadingPreferences.theme,
    )
    val backgroundColor by animateColorAsState(
        Color(themeValues.intBackgroundColor),
        label = "backgroundColor",
    )
    val baseTextColor by animateColorAsState(
        Color(themeValues.intBaseTextColor),
        label = "baseTextColor",
    )
    val textColor by animateColorAsState(
        Color(themeValues.intTextColor),
        label = "textColor",
    )

    val fontFamily = currentReadingPreferences.fontFamily.toComposeFontFamily()
    val fontSize = currentReadingPreferences.fontSize.toSp()
    val fontSizeMultiplier = currentReadingPreferences.fontSize.multiplier

    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MainTopAppBar(
            title = null,
            navigationIcon = NavigationIcons.CloseIcon,
            onNavigationIconClick = onCloseClick,
            backgroundColor = backgroundColor,
            contentColor = baseTextColor,
            actions = {
                if (showSaveAction) {
                    IconButton(onClick = onSaveClick) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(
                                R.string.save
                            ),
                        )
                    }
                }
            },
        )

        // Preview section
        Column(
            modifier = Modifier
                .background(backgroundColor)
                .fillMaxWidth()
                .padding(
                    bottom = Margin.ExtraLarge.value,
                    start = Margin.ExtraLarge.value,
                    end = Margin.ExtraLarge.value
                ),
            verticalArrangement = Arrangement.spacedBy(
                Margin.ExtraLarge.value,
                Alignment.CenterVertically,
            ),
        ) {
            // Title
            Text(
                text = stringResource(
                    R.string.reader_preferences_screen_preview_title
                ),
                style = getTitleTextStyle(
                    fontFamily, fontSizeMultiplier, baseTextColor
                ),
            )

            // Content
            val contentStyle = TextStyle(
                fontFamily = fontFamily,
                fontSize = fontSize,
                fontWeight = FontWeight.Normal,
                color = textColor,
                lineHeight = fontSize * TEXT_LINE_HEIGHT_MULTIPLIER,
            )

            Text(
                text = stringResource(
                    R.string.reader_preferences_screen_preview_text
                ),
                style = contentStyle,
            )

            // Tags
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    Margin.Medium.value
                ),
                verticalArrangement = Arrangement.spacedBy(
                    Margin.Medium.value
                ),
            ) {
                stringResource(
                    R.string.reader_preferences_screen_preview_tags
                )
                    .split(",")
                    .forEach { tag ->
                        ReadingPreferencesPreviewTag(
                            text = tag.trim(),
                            baseTextColor = baseTextColor,
                            fontSizeMultiplier = fontSizeMultiplier,
                            fontFamily = fontFamily,
                        )
                    }
            }
        }

        // Preferences section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Margin.ExtraMediumLarge.value),
            verticalArrangement = Arrangement.spacedBy(
                Margin.ExtraMediumLarge.value,
                Alignment.CenterVertically,
            ),
        ) {
            // Theme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(
                    Margin.Small.value
                ),
            ) {
                Spacer(
                    modifier = Modifier.width(Margin.ExtraLarge.value)
                )

                ReaderReadingPreferences.Theme.entries.forEach { theme ->
                    ReadingPreferencesThemeButton(
                        theme = theme,
                        isSelected = theme == currentReadingPreferences.theme,
                        onClick = {
                            haptics.performHapticFeedback(
                                HapticFeedbackType.LongPress
                            )
                            onThemeClick(theme)
                        },
                    )
                }

                Spacer(
                    modifier = Modifier.width(Margin.ExtraLarge.value)
                )
            }

            // Font family
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(
                    Margin.Small.value
                ),
            ) {
                Spacer(
                    modifier = Modifier.width(Margin.ExtraLarge.value)
                )

                ReaderReadingPreferences.FontFamily.entries.forEach {
                    fontFamilyEntry ->
                    ReadingPreferencesFontFamilyButton(
                        fontFamily = fontFamilyEntry,
                        isSelected = fontFamilyEntry ==
                            currentReadingPreferences.fontFamily,
                        onClick = {
                            haptics.performHapticFeedback(
                                HapticFeedbackType.LongPress
                            )
                            onFontFamilyClick(fontFamilyEntry)
                        },
                    )
                }

                Spacer(
                    modifier = Modifier.width(Margin.ExtraLarge.value)
                )
            }

            // Font size
            ReadingPreferencesFontSizeSlider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Margin.ExtraLarge.value),
                previewFontFamily = fontFamily,
                selectedFontSize = currentReadingPreferences.fontSize,
                onFontSizeSelected = {
                    haptics.performHapticFeedback(
                        HapticFeedbackType.LongPress
                    )
                    onFontSizeClick(it)
                },
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

@Preview
@Composable
private fun ReadingPreferencesScreenPreview() {
    AppThemeM3 {
        var readingPreferences by remember {
            mutableStateOf(ReaderReadingPreferences())
        }

        ReadingPreferencesScreen(
            currentReadingPreferences = readingPreferences,
            showSaveAction = true,
            onCloseClick = {},
            onSaveClick = {},
            onThemeClick = {
                readingPreferences = readingPreferences.copy(theme = it)
            },
            onFontFamilyClick = {
                readingPreferences = readingPreferences.copy(fontFamily = it)
            },
            onFontSizeClick = {
                readingPreferences = readingPreferences.copy(fontSize = it)
            },
        )
    }
}
