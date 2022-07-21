package org.wordpress.android.util.format

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
    fun format(value: Int): String {
        val percentFormatter = NumberFormat.getPercentInstance(localeManagerWrapper.getLocale())
        return percentFormatter.format(value)
    }
}
