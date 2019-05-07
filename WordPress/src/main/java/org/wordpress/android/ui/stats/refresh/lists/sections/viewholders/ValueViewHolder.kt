package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView.LayoutParams
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.dimen
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.NEGATIVE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.NEUTRAL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.POSITIVE

class ValueViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
        parent,
        layout.stats_block_value_item
) {
    private val container = itemView.findViewById<LinearLayout>(id.value_container)
    private val value = itemView.findViewById<TextView>(id.value)
    private val unit = itemView.findViewById<TextView>(id.unit)
    private val change = itemView.findViewById<TextView>(id.change)
    fun bind(item: ValueItem) {
        value.text = item.value
        unit.setText(item.unit)
        val hasChange = item.change != null
        val color = when (item.state) {
            POSITIVE -> R.color.success_500
            NEGATIVE -> R.color.error_500
            NEUTRAL -> R.color.gray_500
        }
        change.setTextColor(ContextCompat.getColor(change.context, color))
        change.visibility = if (hasChange) View.VISIBLE else View.GONE
        change.text = item.change
        val params = container.layoutParams as LayoutParams
        val topMargin = if (item.isFirst) container.resources.getDimensionPixelSize(dimen.margin_medium) else 0
        params.setMargins(0, topMargin, 0, 0)
        container.layoutParams = params
    }
}
