package org.wordpress.android.ui.newstats.subscribers.subscribersgraph

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.SubscribersGraphResult
import org.wordpress.android.ui.newstats.subscribers.BaseSubscribersCardViewModel
import org.wordpress.android.ui.newstats.util.formatCustomDateRange
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SubscribersGraphViewModel @Inject constructor(
    selectedSiteRepository: SelectedSiteRepository,
    accountStore: AccountStore,
    statsRepository: StatsRepository,
    resourceProvider: ResourceProvider
) : BaseSubscribersCardViewModel<SubscribersGraphUiState>(
    selectedSiteRepository,
    accountStore,
    statsRepository,
    resourceProvider,
    SubscribersGraphUiState.Loading
) {
    private val _selectedTab =
        MutableStateFlow(SubscribersGraphTab.DAYS)
    val selectedTab: StateFlow<SubscribersGraphTab> =
        _selectedTab.asStateFlow()

    override val loadingState = SubscribersGraphUiState.Loading

    override fun errorState(
        message: String,
        isAuthError: Boolean
    ) = SubscribersGraphUiState.Error(message, isAuthError)

    fun onTabSelected(tab: SubscribersGraphTab) {
        if (tab == _selectedTab.value) return
        _selectedTab.value = tab
        resetLoadedSuccessfully()
        loadData()
    }

    override suspend fun loadDataInternal(siteId: Long) {
        val tab = _selectedTab.value
        val today = LocalDate.now()
        val dateStr = today.format(
            DateTimeFormatter.ISO_LOCAL_DATE
        )
        when (
            val result = statsRepository
                .fetchSubscribersGraph(
                    siteId,
                    unit = tab.unit,
                    quantity = tab.quantity,
                    date = dateStr
                )
        ) {
            is SubscribersGraphResult.Success -> {
                markLoadedSuccessfully()
                val sorted = result.dataPoints
                    .map {
                        ParsedPoint(
                            raw = it.date,
                            start = parsePeriodDate(it.date),
                            count = it.count
                        )
                    }
                    .sortedWith(
                        compareBy(nullsLast()) { it.start }
                    )
                updateState(
                    SubscribersGraphUiState.Loaded(
                        dataPoints = sorted.map {
                            GraphDataPoint(
                                label = formatLabel(it, tab),
                                markerLabel =
                                    formatMarkerLabel(it, tab),
                                count = it.count
                            )
                        }
                    )
                )
            }
            is SubscribersGraphResult.Error -> {
                updateState(
                    SubscribersGraphUiState.Error(
                        message = resourceProvider
                            .getString(result.messageResId),
                        isAuthError = result.isAuthError
                    )
                )
            }
        }
    }

    /**
     * Formats one chart point's axis label. `/stats/subscribers` labels every point to match
     * the unit that was asked for, and the shapes do not agree: `2026-01-27` for a day,
     * `2026W01W26` for a week, the first of the month for a month and a bare `2026` for a
     * year. Only the day shape is an ISO date, so the others are parsed on their own terms.
     *
     * A point whose period matched no known shape keeps its raw string, which is the most
     * honest thing left to show.
     */
    private fun formatLabel(
        point: ParsedPoint,
        tab: SubscribersGraphTab
    ): String {
        val start = point.start ?: return point.raw
        val pattern = when (tab) {
            SubscribersGraphTab.DAYS,
            SubscribersGraphTab.WEEKS -> DAY_PATTERN
            SubscribersGraphTab.MONTHS -> MONTH_PATTERN
            SubscribersGraphTab.YEARS -> YEAR_PATTERN
        }
        return start.format(
            DateTimeFormatter.ofPattern(
                pattern, Locale.getDefault()
            )
        )
    }

    /**
     * Formats the label shown when a point is selected. A weekly point spans seven days, so it
     * is named as a range ("27 Jul - 2 Aug") rather than by its first day alone — otherwise the
     * marker is indistinguishable from the Days tab's, which is what CMM-2474 asked for. Every
     * other granularity names a span the axis label already states unambiguously, so it reuses
     * that label.
     */
    private fun formatMarkerLabel(
        point: ParsedPoint,
        tab: SubscribersGraphTab
    ): String {
        val start = point.start
        if (tab != SubscribersGraphTab.WEEKS || start == null) {
            return formatLabel(point, tab)
        }
        return formatCustomDateRange(
            start, start.plusDays(DAYS_IN_WEEK - 1)
        )
    }

    /**
     * Resolves a period string to the first day of the span it names, or `null` when the
     * string is in none of the shapes the endpoint is known to return.
     *
     * A weekly period is not a date in any format: it carries the year, then the month and
     * the day of the week's first day, each prefixed with a `W`. `2026W01W26` is the week
     * starting Monday 26 January 2026 — the embedded day is always a Monday, which is why
     * the same shape is parsed as `yyyy'W'MM'W'dd` in [WeeklyRoundupUtils].
     */
    private fun parsePeriodDate(dateStr: String): LocalDate? = try {
        WEEK_PERIOD_REGEX.matchEntire(dateStr)
            ?.destructured
            ?.let { (year, month, day) ->
                LocalDate.of(
                    year.toInt(), month.toInt(), day.toInt()
                )
            }
            ?: MONTH_PERIOD_REGEX.matchEntire(dateStr)
                ?.destructured
                ?.let { (year, month) ->
                    LocalDate.of(
                        year.toInt(), month.toInt(), FIRST_DAY
                    )
                }
            ?: YEAR_PERIOD_REGEX.matchEntire(dateStr)
                ?.let {
                    LocalDate.of(
                        it.value.toInt(),
                        FIRST_MONTH,
                        FIRST_DAY
                    )
                }
            ?: LocalDate.parse(
                dateStr, DateTimeFormatter.ISO_LOCAL_DATE
            )
    } catch (e: DateTimeException) {
        AppLog.e(
            T.STATS,
            "SubscribersGraphViewModel: unparseable " +
                "period '$dateStr'",
            e
        )
        null
    }

    companion object {
        private const val DAY_PATTERN = "MMM d"
        private const val MONTH_PATTERN = "MMM"
        private const val YEAR_PATTERN = "yyyy"

        private const val FIRST_MONTH = 1
        private const val FIRST_DAY = 1
        private const val DAYS_IN_WEEK = 7L

        private val WEEK_PERIOD_REGEX =
            Regex("""(\d{4})W(\d{2})W(\d{2})""")
        private val MONTH_PERIOD_REGEX =
            Regex("""(\d{4})-(\d{2})""")
        private val YEAR_PERIOD_REGEX = Regex("""\d{4}""")
    }
}

/**
 * One data point with its period resolved once: [start] is the first day of the span [raw]
 * names, or `null` when [raw] matched no known shape. Parsing up front lets the chart be
 * ordered by a real date rather than by how the period happens to sort as text.
 */
private data class ParsedPoint(
    val raw: String,
    val start: LocalDate?,
    val count: Long
)
