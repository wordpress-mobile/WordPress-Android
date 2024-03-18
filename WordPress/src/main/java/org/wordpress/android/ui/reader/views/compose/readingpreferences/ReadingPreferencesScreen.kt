package org.wordpress.android.ui.reader.views.compose.readingpreferences

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.postDelayed
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.reader.discover.interests.TagUiState
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.utils.toComposeFontFamily
import org.wordpress.android.ui.reader.utils.toSp
import org.wordpress.android.ui.reader.views.ReaderExpandableTagsView

private const val TITLE_BASE_FONT_SIZE_SP = 24
private const val TITLE_LINE_HEIGHT_MULTIPLIER = 1.2f
private const val TEXT_LINE_HEIGHT_MULTIPLIER = 1.6f

// list of TagUiStates with: dogs, fox, design, writing
private val previewTags = listOf("dogs", "fox", "design", "writing")
    .map { tag -> TagUiState(tag, tag) }

@Composable
fun ReadingPreferencesScreen(
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
            verticalArrangement = Arrangement.spacedBy(Margin.ExtraLarge.value, Alignment.CenterVertically),
        ) {
            // title
            Text(
                text = stringResource(R.string.reader_preferences_screen_preview_title),
                style = getTitleTextStyle(fontFamily, fontSizeMultiplier, baseTextColor),
            )

            // tags
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    FrameLayout(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                    }
                },
                update = {
                    // TODO thomashortadev this is not looking so great since sometimes we can notice
                    //   the tag section being removed and then added again. It works though.
                    it.removeAllViews()

                    val contextThemeWrapper = ContextThemeWrapper(it.context, currentReadingPreferences.theme.style)
                    val tagsView = ReaderExpandableTagsView(contextThemeWrapper).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }

                    it.addView(tagsView)

                    it.post {
                        tagsView.updateUi(previewTags, currentReadingPreferences)
                    }
                }
            )

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
            verticalArrangement = Arrangement.spacedBy(Margin.ExtraExtraMediumLarge.value, Alignment.CenterVertically),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Margin.Small.value),
            ) {
                Spacer(modifier = Modifier.width(Margin.ExtraLarge.value))

                ReaderReadingPreferences.Theme.values().forEach { theme ->
                    ReadingPreferencesThemeButton(
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
                    ReadingPreferencesFontFamilyButton(
                        fontFamily = fontFamily,
                        isSelected = fontFamily == currentReadingPreferences.fontFamily,
                        onClick = { onFontFamilyClick(fontFamily) },
                    )
                }

                Spacer(modifier = Modifier.width(Margin.ExtraLarge.value))
            }

            ReadingPreferencesFontSizeSlider(
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

@Preview
@Composable
private fun ReadingPreferencesScreenPreview() {
    AppTheme {
        var readingPreferences by remember { mutableStateOf(ReaderReadingPreferences()) }

        ReadingPreferencesScreen(
            currentReadingPreferences = readingPreferences,
            onCloseClick = {},
            onThemeClick = { readingPreferences = readingPreferences.copy(theme = it) },
            onFontFamilyClick = { readingPreferences = readingPreferences.copy(fontFamily = it) },
            onFontSizeClick = { readingPreferences = readingPreferences.copy(fontSize = it) },
        )
    }
}
