package org.wordpress.android.ui.stats.refresh.utils

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem.Bar
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.text.DecimalFormat
import java.util.TreeMap
import javax.inject.Inject
import kotlin.math.abs

const val ONE_THOUSAND = 1000
const val TEN_THOUSAND = 10000
const val HUNDRED_THOUSAND = 100000
const val MILLION = 1000000

class StatsUtils
@Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val localeManager: LocaleManagerWrapper
) {
    private val suffixes = TreeMap(
            mapOf(
                    1_000L to R.string.suffix_1_000,
                    1_000_000L to R.string.suffix_1_000_000,
                    1_000_000_000L to R.string.suffix_1_000_000_000,
                    1_000_000_000_000L to R.string.suffix_1_000_000_000_000,
                    1_000_000_000_000_000L to R.string.suffix_1_000_000_000_000_000,
                    1_000_000_000_000_000_000L to R.string.suffix_1_000_000_000_000_000_000
            )
    )

    fun toFormattedString(number: Long?, startValue: Int = TEN_THOUSAND): String? {
        return number?.let { toFormattedString(it, startValue) }
    }

    fun toFormattedString(number: Long?, startValue: Int = TEN_THOUSAND, defaultValue: String): String {
        return number?.let { toFormattedString(it, startValue) } ?: defaultValue
    }

    fun toFormattedString(number: Double?, startValue: Int = TEN_THOUSAND): String? {
        return number?.let { toFormattedString(it, startValue) }
    }

    fun toFormattedString(number: Double?, startValue: Int = TEN_THOUSAND, defaultValue: String): String {
        return number?.let { toFormattedString(it, startValue) } ?: defaultValue
    }

    fun toFormattedString(number: Double, startValue: Int = TEN_THOUSAND): String {
        return number.let {
            if (it < startValue && it > (startValue * -1)) {
                val formatter = DecimalFormat.getInstance(localeManager.getLocale())
                formatter.maximumFractionDigits = 1
                formatter.minimumFractionDigits = 0
                formatter.format(it)
            } else {
                toFormattedString(it.toLong(), startValue)
            }
        }
    }

    fun toFormattedString(number: Int?, startValue: Int = TEN_THOUSAND): String? {
        return toFormattedString(number?.toLong(), startValue)
    }

    fun toFormattedString(number: Int?, startValue: Int = TEN_THOUSAND, defaultValue: String): String {
        return toFormattedString(number?.toLong(), startValue, defaultValue)
    }

    fun toFormattedString(number: Int, startValue: Int = TEN_THOUSAND): String {
        return toFormattedString(number.toLong(), startValue)
    }

    fun toFormattedString(
        number: Long,
        startValue: Int = TEN_THOUSAND
    ): String {
        val isNegative = number < 0
        val safeNumber = abs(
                if (number == java.lang.Long.MIN_VALUE) {
                    number + 1
                } else {
                    number
                }
        )
        if (safeNumber < startValue) {
            return printNumber(safeNumber, isNegative)
        }

        val e = suffixes.floorEntry(safeNumber)
        val divideBy = e.key
        val suffix = e.value

        val truncated = safeNumber / (divideBy!! / 10)
        val hasDecimal = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
        return printNumber(truncated, isNegative, suffix, hasDecimal)
    }

    private fun printNumber(
        number: Long,
        isNegative: Boolean,
        suffix: Int? = null,
        hasDecimal: Boolean = false
    ): String {
        val formattedNumber =
                DecimalFormat.getInstance(localeManager.getLocale()).format(
                        when {
                            suffix != null && hasDecimal -> number / 10.0
                            suffix != null -> number / 10
                            else -> number
                        }
                )
        return if (suffix != null) {
            resourceProvider.getString(
                    suffix,
                    handleNegativeNumber(
                            formattedNumber,
                            isNegative
                    )
            )
        } else {
            handleNegativeNumber(
                    formattedNumber,
                    isNegative
            )
        }
    }

    private fun handleNegativeNumber(number: String, isNegative: Boolean): String {
        return if (isNegative) {
            resourceProvider.getString(R.string.negative_prefix, number)
        } else {
            number
        }
    }

    fun getBarChartEntryContentDescriptions(
        @StringRes entryType: Int,
        entries: List<Bar>,
        @StringRes overlappingEntryType: Int? = null,
        overlappingEntries: List<Bar>? = null
    ): List<String> {
        val contentDescriptions = mutableListOf<String>()
        entries.forEachIndexed { index, bar ->
            var contentDescription = resourceProvider.getString(
                    R.string.stats_bar_chart_accessibility_entry,
                    bar.label,
                    bar.value,
                    resourceProvider.getString(entryType)
            )

            overlappingEntries?.getOrNull(index)?.let { overlappingBar ->
                overlappingEntryType?.let {
                    contentDescription += resourceProvider.getString(
                            R.string.stats_bar_chart_accessibility_overlapping_entry,
                            overlappingBar.value,
                            resourceProvider.getString(overlappingEntryType)
                    )
                }
            }

            contentDescriptions.add(contentDescription)
        }
        return contentDescriptions
    }
}

fun getBarWidth(views: Int, maxViews: Int) = getBarWidth(views.toDouble(), maxViews.toDouble())

fun getBarWidth(views: Long, maxViews: Long) = getBarWidth(views.toDouble(), maxViews.toDouble())

private fun getBarWidth(
    views: Double,
    maxViews: Double
): Int? {
    return if (maxViews > 0) {
        ((views / maxViews) * 100).toInt()
    } else {
        null
    }
}
