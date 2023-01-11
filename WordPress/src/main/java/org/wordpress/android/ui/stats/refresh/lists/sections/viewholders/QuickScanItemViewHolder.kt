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
    private val startText = itemView.findViewById<TextView>(R.id.start_text)

    private val endValueContainer = itemView.findViewById<LinearLayout>(R.id.end_value_container)
    private val endLabel = itemView.findViewById<TextView>(R.id.end_label)
    private val endValue = itemView.findViewById<TextView>(R.id.end_value)
    private val endText = itemView.findViewById<TextView>(R.id.end_text)

    private val horizontalDivider2 = itemView.findViewById<View>(R.id.horizontal_divider2)
    private val thirdValueContainer = itemView.findViewById<LinearLayout>(R.id.third_value_container)
    private val thirdLabel = itemView.findViewById<TextView>(R.id.third_label)
    private val thirdValue = itemView.findViewById<TextView>(R.id.third_value)
    private val thirdText = itemView.findViewById<TextView>(R.id.third_text)

    fun bind(item: QuickScanItem) {
        bindColumn(item.startColumn, startLabel, startValue, startText, startValueContainer)
        bindColumn(item.endColumn, endLabel, endValue, endText, endValueContainer)
        item.thirdColumn?.let {
            horizontalDivider2.visibility = View.VISIBLE
            thirdValueContainer.visibility = View.VISIBLE
            bindColumn(item.thirdColumn, thirdLabel, thirdValue, thirdText, thirdValueContainer)
        }
    }

    private fun bindColumn(
        column: Column,
        label: TextView,
        value: TextView,
        highest: TextView,
        container: LinearLayout
    ) {
        label.setText(column.label)
        value.text = column.value
        column.highest?.let {
            highest.text = column.highest
            highest.visibility = View.VISIBLE
        }
        column.tooltip?.let {
            TooltipCompat.setTooltipText(container, column.tooltip)
            container.setOnClickListener {
                container.announceForAccessibility(column.tooltip)
                it.performLongClick()
            }
        }
    }
}
