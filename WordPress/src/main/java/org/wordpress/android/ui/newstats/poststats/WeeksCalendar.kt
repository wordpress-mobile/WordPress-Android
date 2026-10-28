package org.wordpress.android.ui.newstats.poststats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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

private const val DAYS_IN_WEEK = 7

/**
 * "Recent Weeks" as a calendar heat grid: one row per week, one square per day, shaded by how busy
 * the day was against the busiest day on show, with the count written in any day that had views.
 *
 * Drawn as a plain [Column] of [Row]s rather than a lazy grid. The grid is a fixed seven columns by
 * however many weeks the API returned -- a few dozen cells -- so laziness buys nothing, and the
 * card is itself one item of the screen's `LazyColumn`, where a nested lazy grid would need a
 * height given to it up front. Weights give the columns equal widths at any card width, and the
 * cell's own aspect ratio keeps it square without measuring anything by hand.
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
        RowLabel(text = row.label, width = labelWidth, style = style)
        Spacer(modifier = Modifier.width(LabelGap))
        row.days.forEach { day ->
            HeatCell(
                value = day.views,
                busiest = busiestDay,
                contentDescription = dayDescription(day),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** What a screen reader reads out for a day that had views, and nothing for one that had none. */
@Composable
private fun dayDescription(day: CalendarDay): String? =
    if (day.views > 0) {
        pluralStringResource(
            R.plurals.stats_post_detail_period_views,
            day.views.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            formatStatsDate(day.day),
            formatStatValue(day.views)
        )
    } else {
        null
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
