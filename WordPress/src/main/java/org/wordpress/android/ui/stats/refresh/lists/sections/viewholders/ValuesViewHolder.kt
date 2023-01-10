package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValuesItem

class ValuesViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_values_item
) {
    private val container1 = itemView.findViewById<ViewGroup>(R.id.values_container1)
    private val container2 = itemView.findViewById<ViewGroup>(R.id.values_container2)
    private val value1 = itemView.findViewById<TextView>(R.id.value1)
    private val value2 = itemView.findViewById<TextView>(R.id.value2)
    private val unit1 = itemView.findViewById<TextView>(R.id.unit1)
    private val unit2 = itemView.findViewById<TextView>(R.id.unit2)

    fun bind(item: ValuesItem) {
        value1.text = item.value1
        value2.text = item.value2
        unit1.setText(item.unit1)
        unit2.setText(item.unit2)

        val context = container1.context
        val color = when (item.selectedItem) {
            0 -> context.getColor(R.color.blue_50)
            1 -> context.getColor(R.color.purple_50)
            else -> context.getColor(R.color.blue_50)
        }

        value1.setTextColor(color)
        unit1.setTextColor(color)

        container1.contentDescription = item.contentDescription1
        container2.contentDescription = item.contentDescription2
    }
}
