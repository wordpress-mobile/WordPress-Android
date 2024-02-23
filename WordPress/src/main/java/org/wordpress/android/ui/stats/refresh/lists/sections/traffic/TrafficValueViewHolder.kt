package org.wordpress.android.ui.stats.refresh.lists.sections.traffic

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.NEGATIVE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.NEUTRAL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem.State.POSITIVE
import org.wordpress.android.ui.stats.refresh.lists.sections.viewholders.BlockListItemViewHolder
import org.wordpress.android.util.extensions.getColorResIdFromAttribute
import org.wordpress.android.util.extensions.getString

class TrafficValueViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_traffic_value_item
) {
    private val container = itemView.findViewById<LinearLayout>(R.id.value_container)
    private val value = itemView.findViewById<TextView>(R.id.value)
    private val unit = itemView.findViewById<TextView>(R.id.unit)
    private val change = itemView.findViewById<TextView>(R.id.change)
    private val period = itemView.findViewById<TextView>(R.id.period)
    fun bind(item: BlockListItem.ValueItem) {
        value.text = item.value
        unit.setText(item.unit)
        val hasChange = item.change != null
        val color = when (item.state) {
            POSITIVE -> change.context.getColorResIdFromAttribute(R.attr.wpColorSuccess)
            NEGATIVE -> change.context.getColorResIdFromAttribute(R.attr.wpColorError)
            NEUTRAL -> change.context.getColorResIdFromAttribute(R.attr.wpColorOnSurfaceMedium)
        }
        val granularity = if (item.period != 0) itemView.getString(item.period) else ""
        val periodText = when (item.state) {
            POSITIVE -> String.format(itemView.getString(R.string.stats_traffic_change_higher), granularity)
            NEGATIVE -> String.format(itemView.getString(R.string.stats_traffic_change_lower), granularity)
            NEUTRAL -> ""
        }

        change.setTextColor(AppCompatResources.getColorStateList(change.context, color))
        change.visibility = if (hasChange) View.VISIBLE else View.GONE
        change.text = item.change
        period.text = periodText
        val params = container.layoutParams as RecyclerView.LayoutParams
        val topMargin = if (item.isFirst) container.resources.getDimensionPixelSize(R.dimen.margin_medium) else 0
        params.setMargins(0, topMargin, 0, 0)
        container.layoutParams = params
        container.contentDescription = item.contentDescription
    }
}
