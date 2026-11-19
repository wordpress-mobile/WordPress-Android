package org.wordpress.android.ui.newstats

import org.wordpress.android.ui.newstats.datasource.StatsUnit

/**
 * The bucket width the Views chart draws, as the user chose it.
 *
 * [AUTOMATIC] is the default and keeps the derived behaviour: the window decides, which is what the
 * chart has always done. Every other entry overrides that derivation with its own [unit] — which is
 * the whole mechanism, and why [unit] is nullable.
 *
 * [AUTOMATIC] is never shown: the menu lists units, and the one it ticks is the unit the chart is
 * drawn at, so under [AUTOMATIC] the tick sits on whatever the window resolved to. It is the "not
 * chosen" state rather than a choice, which is what lets the chart keep re-deriving its bucket
 * width as the user moves between periods.
 *
 * An override changes only the *bucketing*, never the window. The chart, the bottom row and the
 * list cards are deliberately kept on one window (decoupling them is how CMM-2489 happened), and
 * holding the window still is also what keeps a comparison like-for-like: the previous window is
 * re-sliced at the chosen unit rather than moved. iOS does the same.
 *
 * [storageKey] is persisted per site and doubles as the analytics name, so it must stay stable
 * independently of the enum name — the same contract [StatsMetric][
 * org.wordpress.android.ui.newstats.viewsstats.StatsMetric] follows. "automatic" matches what iOS
 * reports for the default.
 */
enum class ChartInterval(val storageKey: String, val unit: StatsUnit?) {
    AUTOMATIC("automatic", null),
    HOURS("hour", StatsUnit.HOUR),
    DAYS("day", StatsUnit.DAY),
    WEEKS("week", StatsUnit.WEEK),
    MONTHS("month", StatsUnit.MONTH),
    YEARS("year", StatsUnit.YEAR);

    companion object {
        val DEFAULT = AUTOMATIC

        /** The interval [key] was persisted as, or null when it was never persisted or no longer exists. */
        fun fromStorageKey(key: String?): ChartInterval? = entries.firstOrNull { it.storageKey == key }

        /** The interval that pins the chart to [unit]. Every unit has one, so this never fails. */
        fun of(unit: StatsUnit): ChartInterval = entries.first { it.unit == unit }
    }
}
