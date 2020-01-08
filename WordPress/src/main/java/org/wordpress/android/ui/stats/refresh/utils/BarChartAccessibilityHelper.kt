package org.wordpress.android.ui.stats.refresh.utils

import android.graphics.Rect
import android.os.Bundle
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.customview.widget.ExploreByTouchHelper
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

class BarChartAccessibilityHelper(
    private val barChart: BarChart,
    private val dataSet: BarDataSet,
    private val contentDescriptions: List<String>
) : ExploreByTouchHelper(barChart) {

    init {
        barChart.setOnHoverListener { _, event -> dispatchHoverEvent(event) }
        barChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onNothingSelected() {
            }

            override fun onValueSelected(entry: Entry?, highlight: Highlight?) {
                highlight?.let {
                    invalidateVirtualView(it.dataIndex)
                }
            }
        })
    }

    override fun getVirtualViewAt(x: Float, y: Float): Int {
        val entry = barChart.getEntryByTouchPoint(x, y)

        return when {
            entry != null -> {
                dataSet.getEntryIndex(entry as BarEntry?)
            }
            else -> {
                INVALID_ID
            }
        }
    }

    override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>?) {
        for (i in 0..dataSet.entryCount) {
            virtualViewIds?.add(i)
        }
    }

    override fun onPerformActionForVirtualView(
        virtualViewId: Int,
        action: Int,
        arguments: Bundle?
    ): Boolean {
        when (action) {
            AccessibilityNodeInfoCompat.ACTION_CLICK -> {
                invalidateVirtualView(virtualViewId)
                return true
            }
        }

        return false
    }

    override fun onPopulateNodeForVirtualView(
        virtualViewId: Int,
        node: AccessibilityNodeInfoCompat
    ) {
        node.contentDescription = contentDescriptions[virtualViewId]

        barChart.highlighted?.let { highlights ->
            highlights.forEach { highlight ->
                if (highlight.dataIndex == virtualViewId) {
                    node.isSelected = true
                }
            }
        }

        node.addAction(AccessibilityActionCompat.ACTION_CLICK)
        val entryRectF = barChart.getBarBounds(dataSet.getEntryForIndex(virtualViewId))
        val entryRect = Rect()
        entryRectF.round(entryRect)

        node.setBoundsInParent(entryRect)
    }
}
