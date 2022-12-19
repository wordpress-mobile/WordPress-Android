package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import androidx.core.view.children
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Chips

class ChipsViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        R.layout.stats_block_two_chips_item
) {
    private val chipGroup = itemView.findViewById<ChipGroup>(R.id.choice_chip_group)

    fun bind(
        item: Chips
    ) {
        chipGroup.setOnCheckedChangeListener { group, checkedId ->
            val checkedChip = group.findViewById<Chip>(checkedId)
            checkedChip?.announceForAccessibility(group.resources.getString(R.string.stats_graph_updated))
            item.onColumnSelected?.invoke(group.indexOfChild(checkedChip))
        }

        chipGroup.children.forEachIndexed { index, view ->
            val currentColumn = item.chips[index]
            val chip = view as Chip
            chip.isChecked = item.selectedColumn == null || item.selectedColumn == index
            chip.setText(currentColumn.header)
            chip.contentDescription = currentColumn.contentDescription
        }
    }
}
