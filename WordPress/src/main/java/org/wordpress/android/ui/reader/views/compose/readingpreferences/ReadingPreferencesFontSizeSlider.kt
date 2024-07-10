package org.wordpress.android.ui.reader.views.compose.readingpreferences

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences

private val thumbSize = 12.dp
private val materialMinThumbSize = 20.dp
private val thumbPadding = max(0.dp, (materialMinThumbSize - thumbSize) / 2)
private val totalThumbSize = thumbSize + thumbPadding * 2

private val trackHeight = 1.dp
private val stepIndicatorSize = 5.dp

private val sliderTrackColor: Color
    @ReadOnlyComposable
    @Composable
    get() {
        val alpha = 0.4f
        return MaterialTheme.colors.onSurface.copy(alpha = alpha).compositeOver(MaterialTheme.colors.surface)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingPreferencesFontSizeSlider(
    selectedFontSize: ReaderReadingPreferences.FontSize,
    onFontSizeSelected: (ReaderReadingPreferences.FontSize) -> Unit,
    previewFontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = ReaderReadingPreferences.FontSize.values().indexOf(selectedFontSize)
    val maxRange = (ReaderReadingPreferences.FontSize.values().size - 1).toFloat()

    Column(
        modifier = modifier,
    ) {
        FontSizePreviewLabels(selectedFontSize, onFontSizeSelected, previewFontFamily)

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            SliderStepIndicators()

            val interactionSource = remember { MutableInteractionSource() }
            val sliderColors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.onSurface,
            )

            val contentDescriptionLabel = stringResource(R.string.reader_preferences_screen_font_size_label)
            val selectedFontSizeLabel = stringResource(selectedFontSize.displayNameRes)

            Slider(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = contentDescriptionLabel
                        stateDescription = selectedFontSizeLabel
                    },
                value = selectedIndex.toFloat(),
                onValueChange = {
                    val newIndex = it.toInt()
                    if (newIndex != selectedIndex) {
                        onFontSizeSelected(ReaderReadingPreferences.FontSize.values()[newIndex])
                    }
                },
                valueRange = 0f..maxRange,
                steps = ReaderReadingPreferences.FontSize.values().size - 2, // start and end are already steps
                colors = sliderColors,
                interactionSource = interactionSource,
                thumb = {
                    SliderDefaults.Thumb(
                        modifier = Modifier.padding(thumbPadding),
                        thumbSize = DpSize(thumbSize, thumbSize),
                        interactionSource = interactionSource,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colors.onSurface),
                    )
                },
                track = {
                    SliderTrack()
                }
            )
        }
    }
}

@Composable
private fun FontSizePreviewLabels(
    selectedFontSize: ReaderReadingPreferences.FontSize,
    onFontSizeSelected: (ReaderReadingPreferences.FontSize) -> Unit,
    previewFontFamily: FontFamily,
) {
    val sliderPaddingX = with(LocalDensity.current) { totalThumbSize.toPx() / 2 }
    Layout(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
        content = {
            ReaderReadingPreferences.FontSize.values().forEach { fontSize ->
                val isSelected = fontSize == selectedFontSize

                Text(
                    text = stringResource(R.string.reader_preferences_screen_font_size_preview),
                    style = TextStyle(
                        fontFamily = previewFontFamily,
                        fontSize = fontSize.value.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false,
                        ),
                    ),
                    modifier = Modifier
                        .wrapContentWidth()
                        .clickable(
                            interactionSource = MutableInteractionSource(),
                            indication = null,
                        ) { onFontSizeSelected(fontSize) }
                )
            }
        },
    ) { measurables, constraints ->
        val startX = 0 + sliderPaddingX
        val endX = constraints.maxWidth - sliderPaddingX
        val spacingX = (endX - startX) / (measurables.size - 1)

        val placeables = measurables.map { it.measure(constraints) }

        // the last preview is the biggest font, so let's use it to calculate the height
        val height = placeables.last().height

        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                val x = startX + (spacingX * index) - placeable.width / 2
                val y = height - placeable.height

                placeable.placeRelative(x = x.toInt(), y = y)
            }
        }
    }
}

@Composable
private fun SliderStepIndicators() {
    val stepIndicatorColor = sliderTrackColor
    val steps = ReaderReadingPreferences.FontSize.values().size

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(stepIndicatorSize),
    ) {
        val sliderPaddingX = totalThumbSize.toPx() / 2
        val indicatorSize = stepIndicatorSize.toPx()

        val startX = 0 + sliderPaddingX
        val endX = size.width - sliderPaddingX
        val spacingX = (endX - startX) / (steps - 1)

        repeat(steps) { index ->
            drawCircle(
                color = stepIndicatorColor,
                radius = indicatorSize / 2,
                center = Offset(
                    startX + spacingX * index,
                    indicatorSize / 2
                ),
            )
        }
    }
}

@Composable
private fun SliderTrack() {
    val trackColor = sliderTrackColor

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight),
    ) {
        val start = Offset(0f, center.y)
        val end = Offset(size.width, center.y)

        drawLine(
            trackColor,
            start,
            end,
            trackHeight.toPx(),
            StrokeCap.Round
        )
    }
}
