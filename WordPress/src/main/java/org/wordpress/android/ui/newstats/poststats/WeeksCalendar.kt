package org.wordpress.android.ui.newstats.poststats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.annotation.ColorRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.datasource.PostViewsWeek
import org.wordpress.android.ui.newstats.util.formatStatValue
import org.wordpress.android.ui.newstats.util.formatStatsDate
import org.wordpress.android.ui.newstats.util.formatStatsDayMonth
import org.wordpress.android.util.AppLog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val CellCornerRadius = 8.dp
private val CellGap = 3.dp
private val LabelGap = 8.dp
private val LegendSwatchSize = 14.dp
private val LegendGap = 4.dp
private const val DAYS_IN_WEEK = 7

/**
 * The quartiles of the busiest day on show, matching the thresholds the old stats posting-activity
 * grid buckets by, so the app's two heat grids shade alike.
 */
private const val VERY_HIGH_LEVEL = 0.75
private const val HIGH_LEVEL = 0.5
private const val MEDIUM_LEVEL = 0.25

/** Above this a shade counts as a light one, and takes dark ink. */
private const val LUMINANCE_MIDPOINT = 0.5f

/**
 * "Recent Weeks" as a calendar heat grid: one row per week, one square per day, shaded by how busy
 * the day was against the busiest day on show, with the count written in any day that had views.
 *
 * Drawn as a plain [Column] of [Row]s rather than a lazy grid. The grid is a fixed seven columns by
 * however many weeks the API returned -- a few dozen cells -- so laziness buys nothing, and the
 * card is itself one item of the screen's `LazyColumn`, where a nested lazy grid would need a
 * height given to it up front. Weights give the columns equal widths at any card width, and
 * [Modifier.aspectRatio] keeps the cells square without measuring anything by hand.
 */
@Composable
internal fun WeeksCalendar(
    weeks: List<PostViewsWeek>,
    modifier: Modifier = Modifier
) {
    val rows = remember(weeks) { weeks.toCalendarRows() }
    if (rows.isEmpty()) return

    val busiestDay = remember(rows) {
        rows.flatMap { it.days }.maxOfOrNull { it.views } ?: 0L
    }
    val labelStyle = MaterialTheme.typography.labelMedium
    val labelWidth = rememberLabelWidth(
        labels = rows.map { it.label },
        style = labelStyle
    )
    val weekStart = rows.first().weekStart
    val weekdayNames = remember(weekStart) {
        weekStart.namesOfWeek(Locale.getDefault())
    }

    Column(modifier = modifier.fillMaxWidth()) {
        WeekdayHeader(
            names = weekdayNames,
            labelWidth = labelWidth,
            style = labelStyle
        )
        rows.forEach { row ->
            WeekRow(
                row = row,
                busiestDay = busiestDay,
                labelWidth = labelWidth,
                style = labelStyle
            )
        }
        Spacer(modifier = Modifier.height(LabelGap))
        IntensityLegend(style = labelStyle)
    }
}

@Composable
private fun WeekdayHeader(
    names: List<String>,
    labelWidth: Dp,
    style: TextStyle
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = CellGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(labelWidth + LabelGap))
        names.forEach { name ->
            Text(
                text = name,
                style = style,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = CellGap)
            )
        }
    }
}

@Composable
private fun WeekRow(
    row: CalendarRow,
    busiestDay: Long,
    labelWidth: Dp,
    style: TextStyle
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.label,
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(labelWidth)
        )
        Spacer(modifier = Modifier.width(LabelGap))
        row.days.forEach { day ->
            DayCell(
                day = day,
                busiestDay = busiestDay,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * One day's square. A day with views names itself and its count; an empty one is left out of the
 * accessibility tree entirely, so the grid is swiped through in a handful of steps rather than in
 * one per cell.
 */
@Composable
private fun DayCell(
    day: CalendarDay,
    busiestDay: Long,
    modifier: Modifier = Modifier
) {
    val cellColor = intensityColor(day.views, busiestDay)
    val description = if (day.views > 0) {
        pluralStringResource(
            R.plurals.stats_post_detail_day_views,
            day.views.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            formatStatsDate(day.day),
            formatStatValue(day.views)
        )
    } else {
        null
    }
    Box(
        modifier = modifier
            .padding(CellGap)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(CellCornerRadius))
            .background(cellColor)
            .then(
                if (description != null) {
                    Modifier.semantics {
                        contentDescription = description
                    }
                } else {
                    Modifier.clearAndSetSemantics {}
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (day.views > 0) {
            Text(
                text = formatStatValue(day.views),
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
private fun IntensityLegend(style: TextStyle) {
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

/**
 * The width of the widest week label, so every row's grid starts at the same place whatever the
 * locale writes there -- "Oct 26", "26. Okt." and "10月26日" are not the same width, and a column
 * fixed in dp would either clip one or waste space on all the others.
 */
@Composable
private fun rememberLabelWidth(
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
private fun intensityColor(views: Long, busiestDay: Long): Color =
    colorResource(IntensityLevel.of(views, busiestDay).colorResId)

/**
 * Readable ink for a count written on a heat cell. Taken from the shade's own brightness rather
 * than from the level, because the scale inverts between the light and dark themes -- the busiest
 * day is the darkest square in one and the lightest in the other.
 */
private fun contentColorOn(background: Color): Color =
    if (background.luminance() > LUMINANCE_MIDPOINT) Color.Black else Color.White

/**
 * The five shades a day can take, quietest first. These are the colours the old stats
 * posting-activity grid uses, which already have a dark-theme set of their own.
 */
internal enum class IntensityLevel(@ColorRes val colorResId: Int) {
    NONE(R.color.stats_activity_very_low),
    LOW(R.color.stats_activity_low),
    MEDIUM(R.color.stats_activity_medium),
    HIGH(R.color.stats_activity_high),
    VERY_HIGH(R.color.stats_activity_very_high);

    companion object {
        fun of(views: Long, busiestDay: Long): IntensityLevel = when {
            views <= 0 || busiestDay <= 0 -> NONE
            views > busiestDay * VERY_HIGH_LEVEL -> VERY_HIGH
            views > busiestDay * HIGH_LEVEL -> HIGH
            views > busiestDay * MEDIUM_LEVEL -> MEDIUM
            else -> LOW
        }
    }
}

/** A week as the grid draws it: its label and exactly [DAYS_IN_WEEK] squares. */
internal data class CalendarRow(
    val label: String,
    val weekStart: DayOfWeek,
    val days: List<CalendarDay>
)

/**
 * One square: the day it stands for, and its views. A day the API sent nothing for -- one that has
 * not happened yet, in the week still running -- is a zero like any other, and is drawn like one.
 */
internal data class CalendarDay(
    val day: String,
    val views: Long
)

/**
 * Turns the API's weeks into rows of seven.
 *
 * Days are placed by the weekday they actually fall on rather than by their position in the
 * response, so the most recent week -- the only one the API may cut short -- leaves the days that
 * have not happened yet empty at the end of its row instead of shifting the rest left.
 *
 * Which weekday a row starts on is read off the data for the same reason: WordPress.com buckets
 * these weeks from Monday, but a column order hardcoded to Monday would silently mis-lay the grid
 * if that ever changed.
 *
 * Internal, rather than inlined into the composable, so the placement rules above can be tested.
 */
internal fun List<PostViewsWeek>.toCalendarRows(): List<CalendarRow> {
    val weeks = filter { it.days.isNotEmpty() }
    if (weeks.isEmpty()) return emptyList()
    val weekStart = weeks.firstDayOfWeek()

    return weeks.map { week ->
        val viewsByDate = week.days.associate { it.day to it.views }
        val firstDay = week.days.first().day.toLocalDayOrNull()
        // The row is laid out from the week's own first square, which is the first day the API
        // sent only when that day is itself the start of the week.
        val rowStart = firstDay
            ?.with(TemporalAdjusters.previousOrSame(weekStart))
        CalendarRow(
            label = formatStatsDayMonth(week.startDay),
            weekStart = weekStart,
            days = (0 until DAYS_IN_WEEK).map { offset ->
                val date = rowStart?.plusDays(offset.toLong())
                val key = date?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                CalendarDay(
                    day = key.orEmpty(),
                    views = key?.let { viewsByDate[it] } ?: 0L
                )
            }
        )
    }
}

/**
 * Which weekday these weeks are bucketed from. WordPress.com buckets them from Monday, but reading
 * it off the data costs little and keeps the grid correct if that ever changes.
 *
 * Read from a week that has all seven days in it, because the two weeks at the ends of the range
 * can be cut short -- the most recent one by the days that have not happened yet, the oldest by the
 * item not existing before it was published -- and the oldest one's first day is then just the day
 * the item went up, not the start of a week. With no full week to go on, the most common first day
 * is as good as it gets.
 */
private fun List<PostViewsWeek>.firstDayOfWeek(): DayOfWeek =
    filter { it.days.size == DAYS_IN_WEEK }
        .ifEmpty { this }
        .mapNotNull { it.days.first().day.toLocalDayOrNull()?.dayOfWeek }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?: DayOfWeek.MONDAY

/** The seven weekday initials in grid order, starting at this day. */
private fun DayOfWeek.namesOfWeek(locale: Locale): List<String> =
    (0 until DAYS_IN_WEEK).map { offset ->
        plus(offset.toLong()).getDisplayName(JavaTextStyle.NARROW, locale)
    }

@Suppress("SwallowedException", "TooGenericExceptionCaught")
private fun String.toLocalDayOrNull(): LocalDate? = try {
    LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)
} catch (e: Exception) {
    AppLog.w(AppLog.T.STATS, "Failed to parse stats day '$this': ${e.message}")
    null
}
