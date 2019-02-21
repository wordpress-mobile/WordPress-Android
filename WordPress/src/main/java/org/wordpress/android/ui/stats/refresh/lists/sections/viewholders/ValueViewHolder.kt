package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.support.v7.widget.RecyclerView.LayoutParams
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.wordpress.android.R.dimen
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem

class ValueViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_block_value_item
) {
    private val container = itemView.findViewById<LinearLayout>(id.value_container)
    private val value = itemView.findViewById<TextView>(id.value)
    private val unit = itemView.findViewById<TextView>(id.unit)
    private val positiveChange = itemView.findViewById<TextView>(id.positive_change)
    private val negativeChange = itemView.findViewById<TextView>(id.negative_change)
    fun bind(item: ValueItem) {
        value.text = item.value
        unit.setText(item.unit)
        val hasChange = item.change != null
        positiveChange.visibility = if (hasChange && item.positive) View.VISIBLE else View.GONE
        negativeChange.visibility = if (hasChange && !item.positive) View.VISIBLE else View.GONE
        positiveChange.text = item.change
        negativeChange.text = item.change
        val params = container.layoutParams as LayoutParams
        val topMargin = if (item.isFirst) container.resources.getDimensionPixelSize(dimen.margin_medium) else 0
        params.setMargins(0, topMargin, 0, 0)
        container.layoutParams = params
    }
}
