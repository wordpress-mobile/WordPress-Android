package org.wordpress.android.util.text

import android.icu.text.NumberFormat
import org.wordpress.android.util.LocaleManagerWrapper
import java.math.RoundingMode
import javax.inject.Inject

private const val MAXIMUM_FRACTION_DIGITS = 0
private const val FORMAT_DIVISOR = 100

/**
 * Used to format a string with a percent sign (%) based on the locale.
 * @param localeManagerWrapper provides the Locale used to return the localized formatted string
 */
class PercentFormatter @Inject constructor(
    private val localeManagerWrapper: LocaleManagerWrapper
) {
    /**
     * Returns a String with a percent sign (%) using the given Float parameter. The returned String uses the
     * default Locale.
     * @param value the value to be returned formatted
     * @return the formatted string
     */
    fun format(
        value: Float,
        maxFractionDigits: Int = MAXIMUM_FRACTION_DIGITS,
        rounding: RoundingMode = RoundingMode.DOWN
    ): String {
        val percentFormatter = NumberFormat.getPercentInstance(localeManagerWrapper.getLocale()).apply {
            maximumFractionDigits = maxFractionDigits
            roundingMode = rounding.ordinal
        }
        return percentFormatter.format(value)
    }

    /**
     * Returns a String with a percent sign (%) using the given Int parameter. The Int value will be returned as the
     * percentage (e.g. if the Int value is 10, the returned String for Locale.US will be "10%"). The returned String
     * uses the default Locale.
     * @param value the value to be returned formatted
     * @return the formatted string
     */
    fun format(
        value: Int,
        maxFractionDigits: Int = MAXIMUM_FRACTION_DIGITS,
        rounding: RoundingMode = RoundingMode.DOWN
    ) = format(
        value = value.toFloat() / FORMAT_DIVISOR,
        maxFractionDigits,
        rounding
    )
}
