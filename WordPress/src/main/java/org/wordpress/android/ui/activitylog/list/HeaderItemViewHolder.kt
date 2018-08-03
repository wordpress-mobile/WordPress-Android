package org.wordpress.android.ui.activitylog.list

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R

class HeaderItemViewHolder(parent: ViewGroup) : ActivityLogViewHolder(parent, R.layout.activity_log_list_header_item) {
    private val header: TextView = itemView.findViewById(R.id.activity_header_text)

    fun bind(item: ActivityLogListItem.Header) {
        header.text = item.text
    }
}
