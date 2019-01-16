package org.wordpress.android.ui.stats.refresh.lists.viewholders

import android.view.ViewGroup
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.Error

class ErrorViewHolder(val parent: ViewGroup) : BaseStatsViewHolder<Error>(parent, R.layout.stats_error_view) {
    private val title: TextView = itemView.findViewById(R.id.error_message)
    override fun bind(item: Error) {
        super.bind(item)
        if (item.errorMessage.isNotBlank()) {
            title.text = String.format("%s: %s", item.statsTypes.toString(), item.errorMessage)
        } else {
            title.text = String.format("%s: %s",
                    item.statsTypes.toString(),
                    parent.resources.getString(R.string.stats_generic_error)
            )
        }
    }
}
