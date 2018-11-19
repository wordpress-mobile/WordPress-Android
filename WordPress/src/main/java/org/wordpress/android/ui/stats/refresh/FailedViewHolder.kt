package org.wordpress.android.ui.stats.refresh

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.layout

class FailedViewHolder(parent: ViewGroup) : InsightsViewHolder(parent, layout.stats_error_block) {
    private val title: TextView = itemView.findViewById(R.id.error_message)
    fun bind(insightsItem: Failed) {
        title.text = insightsItem.errorMessage
    }
}
