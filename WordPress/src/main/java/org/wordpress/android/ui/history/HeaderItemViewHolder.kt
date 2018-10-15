package org.wordpress.android.ui.history

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R

class HeaderItemViewHolder(parent: ViewGroup) : HistoryViewHolder(parent, R.layout.history_list_header) {
    private val header: TextView = itemView.findViewById(R.id.header_text)

    fun bind(revision: HistoryListItem.Header) {
        header.text = revision.text
    }
}
