package org.wordpress.android.ui.stats.refresh.lists.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.lists.Error

class ErrorViewHolder(parent: ViewGroup) : BaseStatsViewHolder<Error>(parent, layout.stats_error_view) {
    private val title: TextView = itemView.findViewById(R.id.error_message)
    override fun bind(item: Error) {
        super.bind(item)
        title.text = item.errorMessage
    }
}
