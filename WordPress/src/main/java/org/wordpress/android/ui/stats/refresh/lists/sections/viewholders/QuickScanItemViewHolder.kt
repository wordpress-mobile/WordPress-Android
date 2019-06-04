package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem.Column

class QuickScanItemViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_quick_scan_item
) {
    private val startLabel = itemView.findViewById<TextView>(id.start_label)
    private val startValue = itemView.findViewById<TextView>(id.start_value)
    private val endLabel = itemView.findViewById<TextView>(id.end_label)
    private val endValue = itemView.findViewById<TextView>(id.end_value)

    fun bind(item: QuickScanItem) {
        bindColumn(item.startColumn, startLabel, startValue)
        bindColumn(item.endColumn, endLabel, endValue)
    }

    private fun bindColumn(column: Column, label: TextView, value: TextView) {
        label.setText(column.label)
        value.text = column.value
        TooltipCompat.setTooltipText(value, column.tooltip)
        value.setOnClickListener { it.performLongClick() }
    }
}
