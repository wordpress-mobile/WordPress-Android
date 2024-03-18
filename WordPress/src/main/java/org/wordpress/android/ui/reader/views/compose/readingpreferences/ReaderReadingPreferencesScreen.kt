package org.wordpress.android.ui.reader.views.compose.readingpreferences

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences

private const val TITLE_BASE_FONT_SIZE_SP = 24
private const val TITLE_LINE_HEIGHT_MULTIPLIER = 1.2f
private const val TEXT_LINE_HEIGHT_MULTIPLIER = 1.6f

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
                .padding(Margin.ExtraLarge.value),
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
                    lineHeight = fontSize * TEXT_LINE_HEIGHT_MULTIPLIER,
                ),
            )
        }

        // Preferences section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(MaterialTheme.colors.surface)
                .padding(vertical = Margin.ExtraExtraMediumLarge.value),
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Margin.Small.value),
            ) {
                Spacer(modifier = Modifier.width(Margin.ExtraLarge.value))

                ReaderReadingPreferences.Theme.values().forEach { theme ->
                    ReaderReadingPreferencesThemeButton(
                        theme = theme,
                        isSelected = theme == currentReadingPreferences.theme,
                        onClick = { onThemeClick(theme) },
                    )
                }

                Spacer(modifier = Modifier.width(Margin.ExtraLarge.value))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Margin.Small.value),
            ) {
                Spacer(modifier = Modifier.width(Margin.ExtraLarge.value))

                ReaderReadingPreferences.FontFamily.values().forEach { fontFamily ->
                    ReaderReadingPreferencesFontFamilyButton(
                        fontFamily = fontFamily,
                        isSelected = fontFamily == currentReadingPreferences.fontFamily,
                        onClick = { onFontFamilyClick(fontFamily) },
                    )
                }

                Spacer(modifier = Modifier.width(Margin.ExtraLarge.value))
            }

            FontSlider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Margin.ExtraLarge.value),
                selectedFontSize = currentReadingPreferences.fontSize,
                onFontSizeSelected = onFontSizeClick,
                previewFontFamily = fontFamily,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSlider(
    selectedFontSize: ReaderReadingPreferences.FontSize,
    onFontSizeSelected: (ReaderReadingPreferences.FontSize) -> Unit,
    previewFontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    val thumbSize = 20.dp
    val selectedIndex = ReaderReadingPreferences.FontSize.values().indexOf(selectedFontSize)

    Column(
        modifier = modifier,
    ) {
        // Text size previews on top of the slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = thumbSize / 2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            ReaderReadingPreferences.FontSize.values().forEach { fontSize ->
                val isSelected = fontSize == selectedFontSize

                Text(
                    text = "A",
                    style = TextStyle(
                        fontFamily = previewFontFamily,
                        fontSize = fontSize.value.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false,
                        ),
                    ),
                    modifier = Modifier
                        .clickable(
                            interactionSource = MutableInteractionSource(),
                            indication = null,
                        ) { onFontSizeSelected(fontSize) }
                )
            }
        }

        val maxRange = (ReaderReadingPreferences.FontSize.values().size - 1).toFloat()
        val sliderTrackColor = Color(0xFF999999)

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val stepTickSize = 10.dp
            val stepPadding = (thumbSize - stepTickSize) / 2
            // Custom steps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = stepPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(ReaderReadingPreferences.FontSize.values().size) {
                    Box(
                        modifier = Modifier
                            .size(stepTickSize)
                            .background(
                                color = sliderTrackColor,
                                shape = CircleShape,
                            )
                    )
                }
            }

            val interactionSource = remember { MutableInteractionSource() }
            val sliderColors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.onSurface,
                inactiveTrackColor = sliderTrackColor,
                activeTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            )
            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = selectedIndex.toFloat(),
                onValueChange = {
                    val newIndex = it.toInt()
                    if (newIndex != selectedIndex) {
                        onFontSizeSelected(ReaderReadingPreferences.FontSize.values()[newIndex])
                    }
                },
                valueRange = 0f..maxRange,
                steps = ReaderReadingPreferences.FontSize.values().size,
                colors = sliderColors,
                interactionSource = interactionSource,
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        thumbSize = DpSize(thumbSize, thumbSize),
                        colors = sliderColors,
                    )
                }
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
