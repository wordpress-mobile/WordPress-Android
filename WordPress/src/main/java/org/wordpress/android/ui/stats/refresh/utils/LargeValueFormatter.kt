package org.wordpress.android.ui.stats.refresh.utils

import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ViewPortHandler
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.round

/**
 * This class is based on the {@link com.github.mikephil.charting.formatter.LargeValueFormatter} and fixes the issue
 * with Locale other than US (in some languages is the DecimalFormat different).
 */
class LargeValueFormatter : ValueFormatter() {
    private var mSuffix = arrayOf("", "k", "m", "b", "t")
    private var mMaxLength = 5
    private val mFormat: DecimalFormat = DecimalFormat("###E00", DecimalFormatSymbols(Locale.US))

    override fun getFormattedValue(
        value: Float,
        entry: Entry,
        dataSetIndex: Int,
        viewPortHandler: ViewPortHandler
    ): String {
        return makePretty(round(value).toDouble())
    }

    override fun getFormattedValue(value: Float): String {
        return makePretty(round(value).toDouble())
    }

    private fun makePretty(number: Double): String {
        var r = mFormat.format(number)

        val numericValue1 = Character.getNumericValue(r[r.length - 1])
        val numericValue2 = Character.getNumericValue(r[r.length - 2])
        val combined = Integer.valueOf(numericValue2.toString() + "" + numericValue1)

        r = r.replace("E[0-9][0-9]".toRegex(), mSuffix[combined / 3])

        while (r.length > mMaxLength || r.matches("[0-9]+\\.[a-z]".toRegex())) {
            r = r.substring(0, r.length - 2) + r.substring(r.length - 1)
        }

        return r
    }
}
