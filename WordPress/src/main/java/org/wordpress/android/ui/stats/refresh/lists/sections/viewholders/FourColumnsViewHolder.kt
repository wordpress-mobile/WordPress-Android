package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.COLUMNS_VALUE_CHANGED
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.SELECTED_COLUMN_CHANGED
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Columns

class FourColumnsViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_four_columns_item
) {
    private val columnLayouts = listOf<LinearLayout>(
        itemView.findViewById(R.id.column1),
        itemView.findViewById(R.id.column2),
        itemView.findViewById(R.id.column3),
        itemView.findViewById(R.id.column4)
    )

    fun bind(
        item: Columns,
        payloads: List<Any>
    ) {
        val tabSelected = payloads.contains(SELECTED_COLUMN_CHANGED)
        val valuesChanged = payloads.contains(COLUMNS_VALUE_CHANGED)
        when {
            tabSelected -> {
                columnLayouts.forEachIndexed { index, layout ->
                    layout.setSelection(item.selectedColumn == index)
                }
            }
            valuesChanged -> {
                columnLayouts.forEachIndexed { index, layout ->
                    layout.value().text = item.columns[index].value
                }
            }
            else -> {
                columnLayouts.forEachIndexed { index, layout ->
                    layout.setOnClickListener {
                        it.announceForAccessibility(it.resources.getString(R.string.stats_graph_updated))
                        item.onColumnSelected?.invoke(index)
                    }
                    val currentColumn = item.columns[index]
                    layout.key().setText(currentColumn.header)
                    layout.value().text = currentColumn.value
                    layout.setSelection(item.selectedColumn == null || item.selectedColumn == index)
                    layout.contentDescription = currentColumn.contentDescription
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
