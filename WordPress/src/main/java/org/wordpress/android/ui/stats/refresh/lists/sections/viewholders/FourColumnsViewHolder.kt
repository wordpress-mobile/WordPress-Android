package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.COLUMNS_VALUE_CHANGED
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.SELECTED_COLUMN_CHANGED
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns

class FourColumnsViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_block_four_columns_item
) {
    private val columnLayouts = listOf<LinearLayout>(
            itemView.findViewById(id.column1),
            itemView.findViewById(id.column2),
            itemView.findViewById(id.column3),
            itemView.findViewById(id.column4)
    )

    fun bind(
        columns: Columns,
        payloads: List<Any>
    ) {
        val tabSelected = payloads.contains(SELECTED_COLUMN_CHANGED)
        val valuesChanged = payloads.contains(COLUMNS_VALUE_CHANGED)
        when {
            tabSelected -> {
                columnLayouts.forEachIndexed { index, layout ->
                    layout.setSelection(columns.selectedColumn == index)
                }
            }
            valuesChanged -> {
                columnLayouts.forEachIndexed { index, layout ->
                    layout.value().text = columns.values[index]
                }
            }
            else -> {
                columnLayouts.forEachIndexed { index, layout ->
                    layout.setOnClickListener {
                        columns.onColumnSelected?.invoke(index)
                    }
                    layout.key().setText(columns.headers[index])
                    layout.value().text = columns.values[index]
                    layout.setSelection(columns.selectedColumn == null || columns.selectedColumn == index)
                }
            }
        }
    }
    private fun LinearLayout.setSelection(isSelected: Boolean) {
        key().isSelected = isSelected
        value().isSelected = isSelected
        selector().visibility = if (isSelected) View.VISIBLE else View.GONE
    }
    private fun LinearLayout.key(): TextView = this.findViewById(R.id.key)
    private fun LinearLayout.value(): TextView = this.findViewById(R.id.value)
    private fun LinearLayout.selector(): View = this.findViewById(R.id.selector)
}
