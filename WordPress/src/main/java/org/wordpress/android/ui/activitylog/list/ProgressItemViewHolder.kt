package org.wordpress.android.ui.activitylog.list

import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R

class ProgressItemViewHolder(private val parent: ViewGroup) : ActivityLogViewHolder(parent, R.layout.activity_log_list_progress_item) {
    private val summary: TextView = itemView.findViewById(R.id.action_summary)
    private val text: TextView = itemView.findViewById(R.id.action_text)
    private val header: TextView = itemView.findViewById(R.id.activity_header_text)

    fun bind(item: ActivityLogListItem.Progress) {
        summary.text = item.title
        text.text = item.description
        header.text = item.header

        header.visibility = if (item.isHeaderVisible) View.VISIBLE else View.GONE
    }
}
