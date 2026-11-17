package org.wordpress.android.ui.newstats.poststats

import androidx.annotation.ColorRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.util.formatStatValue

/**
 * The parts the per-item screen's two heat grids share: the shading scale, the cell, the legend and
 * the row-label column. [WeeksCalendar] and [MonthsCalendar] differ only in what a cell stands for
 * and how the rows are laid out, so everything that decides how a cell *looks* lives here and is
 * decided once for both.
 */

internal val CellCornerRadius = 8.dp
internal val CellGap = 3.dp
internal val LabelGap = 8.dp
private val LegendSwatchSize = 14.dp
private val LegendGap = 4.dp

/**
 * The quartiles of the busiest period on show, matching the thresholds the old stats
 * posting-activity grid buckets by, so the app's heat grids all shade alike.
 */
private const val VERY_HIGH_LEVEL = 0.75
private const val HIGH_LEVEL = 0.5
private const val MEDIUM_LEVEL = 0.25

/**
 * Above this a shade counts as a light one, and takes dark ink.
 *
 * [Color.luminance] is WCAG *relative* luminance, so the point where a shade's contrast with
 * white equals its contrast with black is sqrt(1.05 * 0.05) - 0.05, not the midpoint of the
 * 0..1 range. Half way up would put white on the mid-tones -- the MEDIUM shade in the light
 * theme and HIGH in the dark one -- at around 2:1.
 */
private const val LUMINANCE_CROSSOVER = 0.179f

/**
 * One square of a heat grid: shaded by [value] against [busiest], with the count written in it when
 * there is one.
 *
 * A period with views names itself through [contentDescription]; an empty one is left out of the
 * accessibility tree entirely, so a grid is swiped through in a handful of steps rather than in one
 * per cell.
 *
 * [blank] holds a cell's place in the grid without drawing it, for a period the data does not
 * reach. It still takes up its square, so the columns either side of it stay in line.
 */
@Composable
internal fun HeatCell(
    value: Long,
    busiest: Long,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    blank: Boolean = false
) {
    val cellColor = if (blank) Color.Transparent else intensityColor(value, busiest)
    Box(
        modifier = modifier
            .padding(CellGap)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(CellCornerRadius))
            .background(cellColor)
            // Clearing rather than merging: the count inside the cell is already part of the
            // description, so left in the tree it would be read out a second time on its own.
            .clearAndSetSemantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (value > 0 && !blank) {
            Text(
                // Abbreviated from a thousand rather than from ten, which is what the rest of
                // this screen uses: a cell is a square with six or seven of them to a row, so it
                // is the one place here that is short of width rather than of precision. The
                // description a screen reader gets carries the figure in full.
                text = formatStatValue(value),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColorOn(cellColor)
            )
        }
    }
}

/**
 * The shading scale, read left to right. It is decorative -- every shade it explains is already
 * written as a number in the grid above -- so it is hidden from screen readers.
 */
@Composable
internal fun IntensityLegend(style: TextStyle) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.stats_post_detail_activity_less),
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IntensityLevel.entries.forEach { level ->
            Box(
                modifier = Modifier
                    .padding(horizontal = LegendGap)
                    .size(LegendSwatchSize)
                    .clip(CircleShape)
                    .background(colorResource(level.colorResId))
            )
        }
        Text(
            text = stringResource(R.string.stats_post_detail_activity_more),
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The label that names a grid row, in the fixed-width column [rememberLabelWidth] measures. */
@Composable
internal fun RowLabel(
    text: String,
    width: Dp,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = style,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        maxLines = 1,
        modifier = modifier.width(width)
    )
}

/**
 * The width of the widest row label, so every row's grid starts at the same place whatever the
 * locale writes there -- "Oct 26", "26. Okt." and "10月26日" are not the same width, and a column
 * fixed in dp would either clip one or waste space on all the others.
 */
@Composable
internal fun rememberLabelWidth(
    labels: List<String>,
    style: TextStyle
): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(labels, style, density) {
        with(density) {
            labels.maxOf { measurer.measure(it, style).size.width }.toDp()
        }
    }
}

@Composable
private fun intensityColor(value: Long, busiest: Long): Color =
    colorResource(IntensityLevel.of(value, busiest).colorResId)

/**
 * Readable ink for a count written on a heat cell. Taken from the shade's own brightness rather
 * than from the level, because the scale inverts between the light and dark themes -- the busiest
 * period is the darkest square in one and the lightest in the other.
 */
private fun contentColorOn(background: Color): Color =
    if (background.luminance() > LUMINANCE_CROSSOVER) Color.Black else Color.White

/**
 * The five shades a cell can take, quietest first. These are the colours the old stats
 * posting-activity grid uses, which already have a dark-theme set of their own.
 */
internal enum class IntensityLevel(@ColorRes val colorResId: Int) {
    NONE(R.color.stats_activity_very_low),
    LOW(R.color.stats_activity_low),
    MEDIUM(R.color.stats_activity_medium),
    HIGH(R.color.stats_activity_high),
    VERY_HIGH(R.color.stats_activity_very_high);

    companion object {
        fun of(value: Long, busiest: Long): IntensityLevel = when {
            value <= 0 || busiest <= 0 -> NONE
            value > busiest * VERY_HIGH_LEVEL -> VERY_HIGH
            value > busiest * HIGH_LEVEL -> HIGH
            value > busiest * MEDIUM_LEVEL -> MEDIUM
            else -> LOW
        }
    }
}
