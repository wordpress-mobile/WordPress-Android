package org.wordpress.android.ui.stats.refresh.utils

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.LineChartItem.Line
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class LineChartLabelFormatter @Inject constructor(
    val entries: List<Line>
) : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        val index = value.toInt()
        return if (index < entries.size) {
            formatLabelDate(parseLabelDate(entries[index].label))
        } else {
            ""
        }
    }

    // 12 Apr 2022
    private fun parseLabelDate(label: String): Date? {
        var labelDate: Date? = null
        val labelParser = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        try {
            labelDate = labelParser.parse(label)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return labelDate
    }

    // Apr 12
    private fun formatLabelDate(date: Date?): String {
        val labelFormatter = SimpleDateFormat("MMM d", Locale.getDefault())
        return date?.let { labelFormatter.format(it) }.toString()
    }
}
