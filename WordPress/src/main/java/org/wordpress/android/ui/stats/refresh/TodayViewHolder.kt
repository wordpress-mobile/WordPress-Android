package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.ViewModelProvider
import android.view.ViewGroup
import org.wordpress.android.R.layout
import org.wordpress.android.ui.stats.refresh.StatsItem.Today

class TodayViewHolder(viewModelProvider: ViewModelProvider, parent: ViewGroup) : StatsViewHolder(
        viewModelProvider,
        parent,
        layout.activity_log_list_event_item
) {
    fun bind(statsItem: Today) {
    }
}
