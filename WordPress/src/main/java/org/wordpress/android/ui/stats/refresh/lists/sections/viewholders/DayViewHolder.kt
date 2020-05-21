package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.HIGH
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.INVISIBLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.LOW
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.MEDIUM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.VERY_HIGH
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.VERY_LOW

class DayViewHolder(parent: ViewGroup) : ViewHolder(
        LayoutInflater.from(parent.context).inflate(
                R.layout.stats_day_activity_box,
                parent,
                false
        )
) {
    fun bind(box: Box) {
        val color = when (box) {
            INVISIBLE -> android.R.color.transparent
            VERY_LOW -> R.color.stats_activity_very_low
            LOW -> R.color.stats_activity_low
            MEDIUM -> R.color.stats_activity_medium
            HIGH -> R.color.stats_activity_high
            VERY_HIGH -> R.color.stats_activity_very_high
        }
        itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, color))
    }
}
