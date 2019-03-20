package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.support.v7.widget.TooltipCompat
import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem.Column

class QuickScanItemViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_quick_scan_item
) {
    private val leftLabel = itemView.findViewById<TextView>(id.left_label)
    private val leftValue = itemView.findViewById<TextView>(id.left_value)
    private val rightLabel = itemView.findViewById<TextView>(id.right_label)
    private val rightValue = itemView.findViewById<TextView>(id.right_value)

    fun bind(item: QuickScanItem) {
        bindColumn(item.leftColumn, leftLabel, leftValue)
        bindColumn(item.rightColumn, rightLabel, rightValue)
    }

    private fun bindColumn(column: Column, label: TextView, value: TextView) {
        label.setText(column.label)
        value.text = column.value
        TooltipCompat.setTooltipText(value, column.tooltip)
        value.setOnClickListener { it.performLongClick() }
    }
}
