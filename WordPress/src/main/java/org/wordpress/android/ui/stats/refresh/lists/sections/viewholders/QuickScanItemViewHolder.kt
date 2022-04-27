package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem.Column

class QuickScanItemViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        R.layout.stats_quick_scan_item
) {
    private val startValueContainer = itemView.findViewById<LinearLayout>(R.id.start_value_container)
    private val startLabel = itemView.findViewById<TextView>(R.id.start_label)
    private val startValue = itemView.findViewById<TextView>(R.id.start_value)
    private val startText = itemView.findViewById<TextView>(R.id.start_text)
    private val endValueContainer = itemView.findViewById<LinearLayout>(R.id.end_value_container)
    private val endLabel = itemView.findViewById<TextView>(R.id.end_label)
    private val endValue = itemView.findViewById<TextView>(R.id.end_value)
    private val endText = itemView.findViewById<TextView>(R.id.end_text)

    fun bind(item: QuickScanItem) {
        bindColumn(item.startColumn, startLabel, startValue, startText, startValueContainer)
        bindColumn(item.endColumn, endLabel, endValue, endText, endValueContainer)
    }

    private fun bindColumn(
        column: Column,
        label: TextView,
        value: TextView,
        text: TextView,
        container: LinearLayout
    ) {
        label.setText(column.label)
        value.text = column.value
        text.text = column.tooltip
        column.tooltip?.let {
            TooltipCompat.setTooltipText(container, column.tooltip)
            container.setOnClickListener {
                container.announceForAccessibility(column.tooltip)
                it.performLongClick()
            }
        }
    }
}
