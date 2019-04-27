package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.support.constraint.ConstraintLayout
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem

class ListItemViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_block_list_item
) {
    private val constraintLayout = itemView.findViewById<ConstraintLayout>(id.stats_block_list_item_constraint_layout)
    private val icon = itemView.findViewById<ImageView>(id.icon)
    private val text = itemView.findViewById<TextView>(id.text)
    private val value = itemView.findViewById<TextView>(id.value)
    private val divider = itemView.findViewById<View>(id.divider)
    private var percentageBar = itemView.findViewById<View>(id.percentage_bar)

    fun bind(item: ListItem) {
        icon.visibility = View.GONE
        text.text = item.text
        value.text = item.value
        divider.visibility = if (item.showDivider) {
            View.VISIBLE
        } else {
            View.GONE
        }

        if(item.percentageOfMaxValue != null) {
            percentageBar.visibility = View.VISIBLE

            val globalLayoutListener = PercentageBarGlobalListener(constraintLayout, text, value, percentageBar, item.percentageOfMaxValue)

            constraintLayout.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        } else {
            percentageBar.visibility = View.GONE
        }
    }
}
