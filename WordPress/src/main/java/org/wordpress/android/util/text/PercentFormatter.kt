package org.wordpress.android.util.text

import android.icu.text.NumberFormat
import org.wordpress.android.util.LocaleManagerWrapper
import javax.inject.Inject

/**
 * Used to format a string with a percent sign (%) based on the locale.
 * @param localeManagerWrapper provides the Locale used to return the localized formatted string
 */
class PercentFormatter @Inject constructor(
    private val localeManagerWrapper: LocaleManagerWrapper
) {

    /**
     * Returns a formatted string with a percent sign (%) based on the locale.
     * @param value the value to be returned formatted
     */
    fun format(value: Float): String {
        val percentFormatter = NumberFormat.getPercentInstance(localeManagerWrapper.getLocale()).apply {
            maximumFractionDigits = MAXIMUM_FRACTION_DIGITS
        }
        return percentFormatter.format(value)
    }
}

private const val MAXIMUM_FRACTION_DIGITS = 0
