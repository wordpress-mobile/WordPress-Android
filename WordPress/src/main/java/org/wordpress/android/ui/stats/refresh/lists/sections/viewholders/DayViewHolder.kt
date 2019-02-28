package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.HIGH
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.INVISIBLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.LOW
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.MEDIUM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.VERY_HIGH
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.VERY_LOW

class DayViewHolder(parent: ViewGroup) : ViewHolder(
        LayoutInflater.from(parent.context).inflate(
                layout.stats_day_activity_box,
                parent,
                false
        )
) {
    fun bind(box: Box) {
        val color = when (box) {
            INVISIBLE -> R.color.transparent
            VERY_LOW -> R.color.grey_lighten_20
            LOW -> R.color.stats_low_activity
            MEDIUM -> R.color.stats_medium_activity
            HIGH -> R.color.stats_high_activity
            VERY_HIGH -> R.color.grey_dark
        }
        itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, color))
    }
}
