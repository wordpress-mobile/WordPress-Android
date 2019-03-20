package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.QuickScanItem

class QuickScanItemViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_quick_scan_item
) {
    private val leftLabel = itemView.findViewById<TextView>(id.left_label)
    private val leftValue = itemView.findViewById<TextView>(id.left_value)
    private val rightLabel = itemView.findViewById<TextView>(id.right_label)
    private val rightValue = itemView.findViewById<TextView>(id.right_value)

    fun bind(item: QuickScanItem) {
        leftLabel.setText(item.leftLabel)
        leftValue.text = item.leftValue
        rightLabel.setText(item.rightLabel)
        rightValue.text = item.rightValue
    }
}
