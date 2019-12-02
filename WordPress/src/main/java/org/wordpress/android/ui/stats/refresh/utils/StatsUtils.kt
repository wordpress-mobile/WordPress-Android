package org.wordpress.android.ui.stats.refresh.utils

import org.wordpress.android.R
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.text.DecimalFormat
import java.util.Locale
import java.util.TreeMap
import javax.inject.Inject

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
        return number?.toFormattedString(resourceProvider, startValue)
    }

    fun toFormattedString(number: Long?, startValue: Int = TEN_THOUSAND, defaultValue: String): String {
        return number?.toFormattedString(resourceProvider, startValue) ?: defaultValue
    }

    fun toFormattedString(number: Long, startValue: Int = TEN_THOUSAND): String {
        return number.toFormattedString(resourceProvider, startValue)
    }

    fun toFormattedString(number: Double?, startValue: Int = TEN_THOUSAND): String? {
        return number?.let { toFormattedString(it, startValue) }
    }

    fun toFormattedString(number: Double?, startValue: Int = TEN_THOUSAND, defaultValue: String): String {
        return number?.let { toFormattedString(it, startValue) } ?: defaultValue
    }

    fun toFormattedString(number: Double, startValue: Int = TEN_THOUSAND): String {
        val locale = localeManager.getLocale()
        return number.let {
            if (it < startValue && it > (startValue * -1)) {
                val formatter = DecimalFormat.getInstance(locale)
                formatter.maximumFractionDigits = 1
                formatter.minimumFractionDigits = 0
                formatter.format(it)
            } else {
                it.toLong().toFormattedString(resourceProvider, startValue, locale)
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

    private fun Long.toFormattedString(
        resourceProvider: ResourceProvider,
        startValue: Int = TEN_THOUSAND,
        locale: Locale = Locale.getDefault()
    ): String {
        if (this == java.lang.Long.MIN_VALUE) return (java.lang.Long.MIN_VALUE + 1).toFormattedString(
                locale = locale,
                resourceProvider = resourceProvider
        )
        if (this < 0) return "-" + (-this).toFormattedString(
                locale = locale,
                resourceProvider = resourceProvider
        )
        if (this < startValue) return DecimalFormat.getInstance(locale).format(this).toString()

        val e = suffixes.floorEntry(this)
        val divideBy = e.key
        val suffix = e.value

        val truncated = this / (divideBy!! / 10)
        val hasDecimal = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
        return if (hasDecimal)
            resourceProvider.getString(
                    suffix,
                    DecimalFormat.getInstance(locale).format(truncated / 10.0)
            )
        else
            resourceProvider.getString(suffix, DecimalFormat.getInstance(locale).format(truncated / 10))
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
