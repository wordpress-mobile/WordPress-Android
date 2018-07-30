package org.wordpress.android.ui.activitylog.list

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R

class ProgressItemViewHolder(parent: ViewGroup) :
        ActivityLogViewHolder(parent, R.layout.activity_log_list_progress_item) {
    private val summary: TextView = itemView.findViewById(R.id.action_summary)
    private val text: TextView = itemView.findViewById(R.id.action_text)

    fun bind(item: ActivityLogListItem.Progress) {
        summary.text = item.title
        text.text = item.description
    }
}
