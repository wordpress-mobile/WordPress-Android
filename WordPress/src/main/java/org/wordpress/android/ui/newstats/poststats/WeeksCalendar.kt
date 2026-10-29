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
import org.wordpress.android.ui.newstats.util.formatStatValueExact
import org.wordpress.android.ui.newstats.util.formatStatsDate
import org.wordpress.android.ui.newstats.util.formatStatsDayMonth
import org.wordpress.android.util.AppLog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
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
    // The reader's own first weekday, not the API's: WordPress.com buckets these weeks from
    // Monday wherever you are, and a grid that drew them that way would start the week on the
    // wrong day for everyone whose calendar starts on Sunday.
    val weekStart = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    val rows = remember(weeks, weekStart) { weeks.toCalendarRows(weekStart) }
    if (rows.isEmpty()) return

    val busiestDay = remember(rows) {
        rows.flatMap { it.days }.maxOfOrNull { it.views } ?: 0L
    }
    val labelStyle = MaterialTheme.typography.labelMedium
    val labelWidth = rememberLabelWidth(
        labels = rows.map { it.label },
        style = labelStyle
    )
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
                modifier = Modifier.weight(1f),
                blank = !day.isInWindow
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
            formatStatValueExact(day.views)
        )
    } else {
        null
    }

/** A week as the grid draws it: its label and exactly [DAYS_IN_WEEK] squares. */
internal data class CalendarRow(
    val label: String,
    val days: List<CalendarDay>
)

/**
 * One square: the day it stands for, and its views.
 *
 * A day the API sent nothing for is a zero like any other and is drawn like one -- that is what
 * the days of the running week that have not happened yet are.
 *
 * [isInWindow] marks the ones at the other end, before the first day we were told anything about:
 * days the item may well have been read on, so a quiet square would be a claim rather than a
 * reading. Those are left blank, as the old stats posting-activity grid leaves the days before the
 * month it draws.
 */
internal data class CalendarDay(
    val day: String,
    val views: Long,
    val isInWindow: Boolean = true
)

/**
 * Turns the API's days into rows of seven, each starting on [weekStart].
 *
 * The rows are built from the days' own dates rather than from the API's weeks. WordPress.com
 * buckets these weeks from Monday wherever the reader is, so in a locale whose week starts on
 * Sunday its buckets are not the rows this grid draws: flattening them and re-chunking by date
 * gives a row that begins on the reader's own first weekday whatever the response was cut into.
 *
 * Placing every day by the date it carries also means the two rows at the ends of the range come
 * out right without being special-cased -- the running week leaves its remaining days empty at the
 * end of the row rather than shifting the rest left, and a row the item was published partway
 * through starts under the weekday it actually went up on.
 *
 * Rows come back newest first, as the grid draws them.
 *
 * Internal, rather than inlined into the composable, so the placement rules above can be tested.
 */
internal fun List<PostViewsWeek>.toCalendarRows(weekStart: DayOfWeek): List<CalendarRow> {
    val viewsByDate = flatMap { it.days }
        .mapNotNull { view ->
            view.day.toLocalDayOrNull()?.let { it to view.views }
        }
        .toMap()
    if (viewsByDate.isEmpty()) return emptyList()

    val firstDay = viewsByDate.keys.min()
    val lastDay = viewsByDate.keys.max()
    return generateSequence(
        firstDay.with(TemporalAdjusters.previousOrSame(weekStart))
    ) { it.plusWeeks(1) }
        .takeWhile { !it.isAfter(lastDay) }
        .map { rowStart ->
            CalendarRow(
                label = formatStatsDayMonth(rowStart.toIsoDay()),
                days = (0 until DAYS_IN_WEEK).map { offset ->
                    val date = rowStart.plusDays(offset.toLong())
                    CalendarDay(
                        day = date.toIsoDay(),
                        views = viewsByDate[date] ?: 0L,
                        isInWindow = !date.isBefore(firstDay)
                    )
                }
            )
        }
        // Newest week at the top, the order the response arrives in and the one the old stats
        // screen draws. Building the rows forwards from the oldest is just how the dates run.
        .toList()
        .asReversed()
}

/** The seven weekday initials in grid order, starting at this day. */
private fun DayOfWeek.namesOfWeek(locale: Locale): List<String> =
    (0 until DAYS_IN_WEEK).map { offset ->
        plus(offset.toLong()).getDisplayName(JavaTextStyle.NARROW, locale)
    }

private fun LocalDate.toIsoDay(): String =
    format(DateTimeFormatter.ISO_LOCAL_DATE)

@Suppress("SwallowedException", "TooGenericExceptionCaught")
private fun String.toLocalDayOrNull(): LocalDate? = try {
    LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)
} catch (e: Exception) {
    AppLog.w(AppLog.T.STATS, "Failed to parse stats day '$this': ${e.message}")
    null
}
