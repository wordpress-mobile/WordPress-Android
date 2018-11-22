package org.wordpress.android.ui.stats.refresh.lists.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.Failed

class FailedViewHolder(parent: ViewGroup) : BaseStatsViewHolder(parent, layout.stats_error_view) {
    private val title: TextView = itemView.findViewById(R.id.error_message)
    fun bind(insightsItem: Failed) {
        title.text = insightsItem.errorMessage
    }
}
