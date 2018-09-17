package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.ViewModelProvider
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.stats.refresh.StatsItem.LatestPostSummary
import org.wordpress.android.ui.stats.refresh.StatsItem.Today
import org.wordpress.android.ui.stats.refresh.StatsType.LATEST_POST_SUMMARY
import org.wordpress.android.ui.stats.refresh.StatsType.TODAY_STATS

class StatsListAdapter(val viewModelProvider: ViewModelProvider) : Adapter<StatsViewHolder>() {
    private val items: MutableList<StatsItem> = mutableListOf()
    fun update(items: List<StatsItem>) {
        val diffResult = DiffUtil.calculateDiff(
                StatsDiffCallback(
                        this.items.toList(),
                        items
                )
        )
        this.items.clear()
        this.items.addAll(items)

        diffResult.dispatchUpdatesTo(this)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatsViewHolder {
        return when (StatsType.values()[viewType]) {
            LATEST_POST_SUMMARY -> LatestPostViewHolder(
                    viewModelProvider,
                    parent
            )
            TODAY_STATS -> TodayViewHolder(
                    viewModelProvider,
                    parent
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: StatsViewHolder, position: Int) {
        val item = items[position]
        when(holder) {
            is LatestPostViewHolder -> holder.bind(item as LatestPostSummary)
            is TodayViewHolder -> holder.bind(item as Today)
        }
    }
}