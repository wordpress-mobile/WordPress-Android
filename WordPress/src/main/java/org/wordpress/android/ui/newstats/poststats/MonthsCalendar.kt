package org.wordpress.android.ui.newstats.poststats

import androidx.annotation.PluralsRes
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
import org.wordpress.android.ui.newstats.datasource.PostViewsYear
import org.wordpress.android.ui.newstats.datasource.PostViewsYearAverage
import org.wordpress.android.ui.newstats.util.formatStatValueExact
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private const val MONTHS_IN_YEAR = 12

/**
 * How many months a row of the grid holds. Twelve across would leave each square too narrow to
 * write a figure in on a phone, so a year is drawn as two rows of six.
 */
private const val MONTHS_PER_ROW = 6

/**
 * A year-by-month calendar heat grid, the sibling of [WeeksCalendar]: one block per year, January
 * to December, shaded by how big the month's figure was against the biggest on show.
 *
 * Used for both "Months and Years" and "Avg. Views Per Day", which differ only in what a month's
 * figure counts -- hence [MonthlyYear] rather than either of the API's own year types.
 *
 * Every year is drawn whole even where the API sent only the months that had figures, so the
 * columns mean the same thing in every block and a quiet month reads as a quiet month rather than
 * as a gap.
 */
@Composable
internal fun MonthsCalendar(
    years: List<MonthlyYear>,
    @PluralsRes descriptionPluralResId: Int,
    modifier: Modifier = Modifier
) {
    val currentYear = remember { LocalDate.now().year }
    val blocks = remember(years, currentYear) { years.toCalendarYears(currentYear) }
    if (blocks.isEmpty()) return

    val busiestMonth = remember(blocks) {
        blocks.flatMap { it.months }.maxOfOrNull { it.views } ?: 0L
    }
    val labelStyle = MaterialTheme.typography.labelMedium
    val labelWidth = rememberLabelWidth(
        labels = blocks.map { it.label },
        style = labelStyle
    )
    val monthNames = remember { monthNames(Locale.getDefault()) }

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            YearBlock(
                block = block,
                monthNames = monthNames,
                busiestMonth = busiestMonth,
                descriptionPluralResId = descriptionPluralResId,
                labelWidth = labelWidth,
                style = labelStyle
            )
        }
        Spacer(modifier = Modifier.height(LabelGap))
        IntensityLegend(style = labelStyle)
    }
}

/**
 * One year: its label in the shared left column, against two rows of six months. The label is
 * centred against the block rather than against either row, so it reads as naming the whole year.
 */
@Composable
private fun YearBlock(
    block: CalendarYear,
    monthNames: List<String>,
    busiestMonth: Long,
    @PluralsRes descriptionPluralResId: Int,
    labelWidth: Dp,
    style: TextStyle
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowLabel(text = block.label, width = labelWidth, style = style)
        Spacer(modifier = Modifier.width(LabelGap))
        Column(modifier = Modifier.weight(1f)) {
            block.months.chunked(MONTHS_PER_ROW).forEach { monthsInRow ->
                MonthHeader(
                    names = monthsInRow.map { monthNames[it.month - 1] },
                    style = style
                )
                MonthRow(
                    months = monthsInRow,
                    year = block.label,
                    monthNames = monthNames,
                    busiestMonth = busiestMonth,
                    descriptionPluralResId = descriptionPluralResId
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(
    names: List<String>,
    style: TextStyle
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = CellGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        names.forEach { name ->
            Text(
                text = name,
                style = style,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = CellGap)
            )
        }
    }
}

@Composable
private fun MonthRow(
    months: List<CalendarMonth>,
    year: String,
    monthNames: List<String>,
    busiestMonth: Long,
    @PluralsRes descriptionPluralResId: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        months.forEach { month ->
            HeatCell(
                value = month.views,
                busiest = busiestMonth,
                contentDescription = monthDescription(
                    month = month,
                    year = year,
                    monthNames = monthNames,
                    pluralResId = descriptionPluralResId
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * What a screen reader reads out for a month with a figure, and nothing for one without. The year
 * is spelled out with it, because the same square means a different month in each block.
 */
@Composable
private fun monthDescription(
    month: CalendarMonth,
    year: String,
    monthNames: List<String>,
    @PluralsRes pluralResId: Int
): String? =
    if (month.views > 0) {
        pluralStringResource(
            pluralResId,
            month.views.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            "${monthNames[month.month - 1]} $year",
            formatStatValueExact(month.views)
        )
    } else {
        null
    }

/** A year as the grid draws it: its label and exactly [MONTHS_IN_YEAR] squares, January first. */
internal data class CalendarYear(
    val label: String,
    val months: List<CalendarMonth>
)

/** One square: the month of the year it stands for, numbered from 1, and its views. */
internal data class CalendarMonth(
    val month: Int,
    val views: Long
)

/** A year's figures broken down by month, keyed 1-12, whatever those figures count. */
internal data class MonthlyYear(
    val year: String,
    val months: Map<Int, Long>
)

// Named apart because the two erase to the same JVM signature.
internal fun List<PostViewsYear>.asMonthlyTotals(): List<MonthlyYear> =
    map { MonthlyYear(year = it.year, months = it.months) }

internal fun List<PostViewsYearAverage>.asMonthlyAverages(): List<MonthlyYear> =
    map { MonthlyYear(year = it.year, months = it.months) }

/**
 * Turns the API's years into blocks of twelve months, most recent year first.
 *
 * [currentYear] always gets a block of its own even when the API sent nothing for it, so the card
 * opens on the year the reader is in rather than on whenever the item last had a view. Months the
 * API left out -- including the ones that have not happened yet -- are zeroes, and are drawn as
 * empty squares like any other quiet month.
 *
 * Internal, rather than inlined into the composable, so those rules can be tested.
 */
internal fun List<MonthlyYear>.toCalendarYears(currentYear: Int): List<CalendarYear> {
    if (isEmpty()) return emptyList()
    val monthsByYear = associate { it.year to it.months }
    return (map { it.year } + currentYear.toString())
        .distinct()
        .sortedDescending()
        .map { year ->
            val months = monthsByYear[year].orEmpty()
            CalendarYear(
                label = year,
                months = (1..MONTHS_IN_YEAR).map { month ->
                    CalendarMonth(month = month, views = months[month] ?: 0L)
                }
            )
        }
}

/**
 * The twelve month names, January first. Short rather than narrow: narrow names collide in
 * English -- January, June and July are all "J" -- and the columns are wide enough for three
 * characters.
 */
private fun monthNames(locale: Locale): List<String> =
    Month.entries.map { it.getDisplayName(JavaTextStyle.SHORT, locale) }
