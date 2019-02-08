package org.wordpress.android.ui.stats.refresh.utils

import java.text.DecimalFormat
import java.util.Locale
import java.util.TreeMap

private val SUFFIXES = TreeMap(mapOf(
        1_000L to "k",
        1_000_000L to "M",
        1_000_000_000L to "G",
        1_000_000_000_000L to "T",
        1_000_000_000_000_000L to "P",
        1_000_000_000_000_000_000L to "E"
))

const val TEN_THOUSAND = 10000
const val HUNDRED_THOUSAND = 100000
const val MILLION = 1000000

fun Int.toFormattedString(startValue: Int = TEN_THOUSAND, locale: Locale = Locale.getDefault()): String {
    return this.toLong().toFormattedString(startValue, locale)
}

fun Long.toFormattedString(startValue: Int = TEN_THOUSAND, locale: Locale = Locale.getDefault()): String {
    if (this == java.lang.Long.MIN_VALUE) return (java.lang.Long.MIN_VALUE + 1).toFormattedString(locale = locale)
    if (this < 0) return "-" + (-this).toFormattedString(locale = locale)
    if (this < startValue) return DecimalFormat.getInstance(locale).format(this).toString()

    val e = SUFFIXES.floorEntry(this)
    val divideBy = e.key
    val suffix = e.value

    val truncated = this / (divideBy!! / 10)
    val hasDecimal = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
    return if (hasDecimal)
        DecimalFormat.getInstance(locale).format(truncated / 10.0) + suffix
    else
        DecimalFormat.getInstance(locale).format(truncated / 10) + suffix
}
