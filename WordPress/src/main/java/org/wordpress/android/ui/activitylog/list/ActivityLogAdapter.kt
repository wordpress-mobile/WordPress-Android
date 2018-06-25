package org.wordpress.android.ui.activitylog.list

import android.content.Context
import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wordpress.android.R.layout

class ActivityLogAdapter(
    context: Context,
    private val itemClickListener: (ActivityLogListItem) -> Unit,
    private val rewindClickListener: (ActivityLogListItem) -> Unit
) : Adapter<ActivityLogViewHolder>() {
    private val list = mutableListOf<ActivityLogListItem>()
    private var layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onBindViewHolder(holder: ActivityLogViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && (payloads[0] as? Bundle)?.size() ?: 0 > 0) {
            val bundle = payloads[0] as Bundle
            if (bundle.containsKey(ActivityLogDiffCallback.LIST_ITEM_HEADER_VISIBILITY_KEY)) {
                holder.header.visibility =
                        if (bundle.getBoolean(ActivityLogDiffCallback.LIST_ITEM_HEADER_VISIBILITY_KEY))
                            View.VISIBLE
                        else
                            View.GONE
            }

            if (bundle.containsKey(ActivityLogDiffCallback.LIST_ITEM_BUTTON_VISIBILITY_KEY)) {
                holder.actionButton.visibility =
                        if (bundle.getBoolean(ActivityLogDiffCallback.LIST_ITEM_BUTTON_VISIBILITY_KEY))
                            View.VISIBLE
                        else
                            View.GONE
            }
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun onBindViewHolder(holder: ActivityLogViewHolder, position: Int) {
        getItem(position)?.let { current ->
            holder.bind(current)
        }
    }

    init {
        setHasStableIds(true)
    }

    internal fun updateList(items: List<ActivityLogListItem>) {
        val diffResult = DiffUtil.calculateDiff(ActivityLogDiffCallback(list, items))
        list.clear()
        list.addAll(items)

        diffResult.dispatchUpdatesTo(this)
    }

    private fun getItem(position: Int): ActivityLogListItem? {
        return if (position < 0 || position >= list.size) null else list[position]
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemId(position: Int): Long {
        return if (list[position] is ActivityLogListItem.Event)
            (list[position] as ActivityLogListItem.Event).activityId.hashCode().toLong()
        else
            list[position].hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityLogViewHolder {
        val view = layoutInflater.inflate(layout.activity_log_list_event_item, parent, false) as ViewGroup
        return ActivityLogViewHolder(view, itemClickListener, rewindClickListener)
    }
}
