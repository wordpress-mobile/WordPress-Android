package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import com.google.android.material.chip.Chip
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.BlockDiffCallback.BlockListPayload.SELECTED_COLUMN_CHANGED
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Chips

class ChipsViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        R.layout.stats_block_two_chips_item
) {
    private val chips = listOf<Chip>(
            itemView.findViewById(R.id.chip1),
            itemView.findViewById(R.id.chip2)
    )

    fun bind(
        item: Chips,
        payloads: List<Any>
    ) {
        val chipSelected = payloads.contains(SELECTED_COLUMN_CHANGED)
        when {
            chipSelected -> {
                chips.forEachIndexed { index, chip ->
                    chip.isSelected = item.selectedColumn == index
                }
            }
            else -> {
                chips.forEachIndexed { index, chip ->
                    chip.setOnCheckedChangeListener { buttonView, _ ->
                        buttonView.announceForAccessibility(buttonView.resources.getString(R.string.stats_graph_updated))
                        item.onColumnSelected?.invoke(index)
                    }
                    val currentColumn = item.chips[index]
                    chip.setText(currentColumn.header)
                    chip.isChecked = item.selectedColumn == null || item.selectedColumn == index
                    chip.contentDescription = currentColumn.contentDescription
                }
            }
        }
    }
}
