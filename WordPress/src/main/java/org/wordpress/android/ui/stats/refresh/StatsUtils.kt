package org.wordpress.android.ui.stats.refresh

import org.wordpress.android.util.LocaleManagerWrapper
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.TreeMap

private val SUFFIXES = TreeMap(mapOf(
        1_000L to "k",
        1_000_000L to "M",
        1_000_000_000L to "G",
        1_000_000_000_000L to "T",
        1_000_000_000_000_000L to "P",
        1_000_000_000_000_000_000L to "E"
))

fun Int.toFormattedString(): String {
    return this.toLong().toFormattedString()
}

fun Long.toFormattedString(): String {
    if (this == java.lang.Long.MIN_VALUE) return (java.lang.Long.MIN_VALUE + 1).toFormattedString()
    if (this < 0) return "-" + (-this).toFormattedString()
    if (this < 1000) return this.toString()

    val e = SUFFIXES.floorEntry(this)
    val divideBy = e.key
    val suffix = e.value

    val truncated = this / (divideBy!! / 10)
    val hasDecimal = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
    return if (hasDecimal)
        DecimalFormat.getInstance().format(truncated / 10.0) + suffix
    else
        DecimalFormat.getInstance().format(truncated / 10) + suffix
}
