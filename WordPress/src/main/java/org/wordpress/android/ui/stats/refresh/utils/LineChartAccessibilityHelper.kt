package org.wordpress.android.ui.stats.refresh.utils

import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.customview.widget.ExploreByTouchHelper
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

class LineChartAccessibilityHelper(
    private val lineChart: LineChart,
    private val contentDescriptions: List<String>,
    private val accessibilityEvent: LineChartAccessibilityEvent
) : ExploreByTouchHelper(lineChart) {
    private val dataSet: ILineDataSet = lineChart.data.dataSets.first()

    interface LineChartAccessibilityEvent {
        fun onHighlight(entry: Entry, index: Int)
    }

    init {
        lineChart.setOnHoverListener { _, event -> dispatchHoverEvent(event) }
    }

    override fun getVirtualViewAt(x: Float, y: Float): Int {
        val entry = lineChart.getEntryByTouchPoint(x, y)

        return when {
            entry != null -> {
                dataSet.getEntryIndex(entry as Entry?)
            }
            else -> {
                INVALID_ID
            }
        }
    }

    override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>?) {
        for (i in 0 until dataSet.entryCount) {
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
                val entry = dataSet.getEntryForIndex(virtualViewId)
                accessibilityEvent.onHighlight(entry, virtualViewId)
                return true
            }
        }

        return false
    }

    @Suppress("DEPRECATION")
    override fun onPopulateNodeForVirtualView(
        virtualViewId: Int,
        node: AccessibilityNodeInfoCompat
    ) {
        node.contentDescription = contentDescriptions[virtualViewId]

        lineChart.highlighted?.let { highlights ->
            highlights.forEach { highlight ->
                if (highlight.dataIndex == virtualViewId) {
                    node.isSelected = true
                }
            }
        }

        node.addAction(AccessibilityActionCompat.ACTION_CLICK)
        val entry = dataSet.getEntryForIndex(virtualViewId)
        val position = lineChart.getPosition(entry, AxisDependency.LEFT)
        val entryRectF = RectF(
                position.x - CIRCLE_RADIUS,
                position.y - CIRCLE_RADIUS,
                position.x + CIRCLE_RADIUS,
                position.y + CIRCLE_RADIUS
        )
        val entryRect = Rect()
        entryRectF.round(entryRect)

        node.setBoundsInParent(entryRect)
    }

    companion object {
        private const val CIRCLE_RADIUS = 10
    }
}
