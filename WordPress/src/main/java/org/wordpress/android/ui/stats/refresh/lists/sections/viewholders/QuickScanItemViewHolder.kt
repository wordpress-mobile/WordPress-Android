package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.View
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

    private val endValueContainer = itemView.findViewById<LinearLayout>(R.id.end_value_container)
    private val endLabel = itemView.findViewById<TextView>(R.id.end_label)
    private val endValue = itemView.findViewById<TextView>(R.id.end_value)

    private val horizontalDivider2 = itemView.findViewById<View>(R.id.horizontal_divider2)
    private val thirdValueContainer = itemView.findViewById<LinearLayout>(R.id.third_value_container)
    private val thirdLabel = itemView.findViewById<TextView>(R.id.third_label)
    private val thirdValue = itemView.findViewById<TextView>(R.id.third_value)

    fun bind(item: QuickScanItem) {
        bindColumn(item.startColumn, startLabel, startValue, startValueContainer)
        bindColumn(item.endColumn, endLabel, endValue, endValueContainer)
        item.thirdColumn?.let {
            horizontalDivider2.visibility = View.VISIBLE
            thirdValueContainer.visibility = View.VISIBLE
            bindColumn(item.thirdColumn, thirdLabel, thirdValue, thirdValueContainer)
        }
    }

    private fun bindColumn(
        column: Column,
        label: TextView,
        value: TextView,
        container: LinearLayout
    ) {
        label.setText(column.label)
        value.text = column.value
        column.tooltip?.let {
            TooltipCompat.setTooltipText(container, column.tooltip)
            container.setOnClickListener {
                container.announceForAccessibility(column.tooltip)
                it.performLongClick()
            }
        }
    }
}
