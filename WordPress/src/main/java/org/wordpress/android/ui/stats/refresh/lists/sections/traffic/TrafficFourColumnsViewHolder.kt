package org.wordpress.android.ui.stats.refresh.lists.sections.traffic

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.BlockListItemViewHolder

class TrafficFourColumnsViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_traffic_four_columns_item
) {
    private val columnLayouts = listOf<LinearLayout>(
        itemView.findViewById(R.id.column1),
        itemView.findViewById(R.id.column2),
        itemView.findViewById(R.id.column3),
        itemView.findViewById(R.id.column4)
    )

    fun bind(
        item: BlockListItem.Columns,
        payloads: List<Any>
    ) {
        val tabSelected = payloads.contains(BlockDiffCallback.BlockListPayload.SELECTED_COLUMN_CHANGED)
        when {
            tabSelected -> {
                columnLayouts.forEachIndexed { index, layout ->
                    layout.setSelection(item.selectedColumn == index)
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
                    layout.setSelection(item.selectedColumn == null || item.selectedColumn == index)
                    layout.contentDescription = currentColumn.contentDescription
                }
            }
        }
    }

    private fun LinearLayout.setSelection(isSelected: Boolean) {
        key().isSelected = isSelected
        selector().visibility = if (isSelected) View.VISIBLE else View.GONE
    }

    private fun LinearLayout.key(): TextView = this.findViewById(R.id.key)
    private fun LinearLayout.selector(): View = this.findViewById(R.id.selector)
}
