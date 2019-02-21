package org.wordpress.android.ui.stats.refresh.utils

import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.Adapter
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.LOADING
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.StatsBlockDiffCallback
import org.wordpress.android.ui.stats.refresh.lists.viewholders.BaseStatsViewHolder
import org.wordpress.android.ui.stats.refresh.lists.viewholders.BlockListViewHolder
import org.wordpress.android.ui.stats.refresh.lists.viewholders.LoadingViewHolder

fun RecyclerView.drawMonthActivity() {
    val spanCount = 7
    this.layoutManager = GridLayoutManager(this.context, spanCount, GridLayoutManager.HORIZONTAL, false)
    this.
}

class MonthActivityAdapter : Adapter<BaseStatsViewHolder>() {
    private var items: List<StatsBlock> = listOf()
    fun update(newItems: List<StatsBlock>) {
        val diffResult = DiffUtil.calculateDiff(
                StatsBlockDiffCallback(
                        items,
                        newItems
                )
        )
        items = newItems

        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseStatsViewHolder {
        return when (Type.values()[viewType]) {
            SUCCESS, ERROR, EMPTY -> BlockListViewHolder(parent, imageManager)
            LOADING -> LoadingViewHolder(parent, imageManager)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: BaseStatsViewHolder, position: Int, payloads: List<Any>) {
        val item = items[position]
        when (holder) {
            is BlockListViewHolder -> holder.bind(item.statsTypes, item.data)
            is LoadingViewHolder -> holder.bind(item.statsTypes, item.data)
        }
    }

    override fun onBindViewHolder(holder: BaseStatsViewHolder, position: Int) {
        onBindViewHolder(holder, position, listOf())
    }
}

class DayViewHolder(parent: ViewGroup) : ViewHolder(
        LayoutInflater.from(parent.context).inflate(
                R.layout.stats_day_activity_box,
                parent,
                false
        )
) {
    fun bind(@ColorRes color: Int) {
        itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, color))
    }
}

