package org.wordpress.android.ui.activitylog.list

import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Event
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Progress
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.ViewType

class ActivityLogAdapter(
    private val itemClickListener: (ActivityLogListItem) -> Unit,
    private val rewindClickListener: (ActivityLogListItem) -> Unit
) : Adapter<ActivityLogViewHolder>() {
    private val list = mutableListOf<ActivityLogListItem>()

    override fun onBindViewHolder(holder: ActivityLogViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isNotEmpty() && (payloads[0] as? Bundle)?.size() ?: 0 > 0) {
            val bundle = payloads[0] as Bundle
            holder.updateChanges(bundle)
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun onBindViewHolder(holder: ActivityLogViewHolder, position: Int) {
        if (holder is EventItemViewHolder) {
            holder.bind(list[position] as Event)
        } else if (holder is ProgressItemViewHolder) {
            holder.bind(list[position] as Progress)
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

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemId(position: Int): Long {
        return if (list[position] is ActivityLogListItem.Event)
            (list[position] as ActivityLogListItem.Event).activityId.hashCode().toLong()
        else
            list[position].hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return list[position].type.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityLogViewHolder {
        return if (viewType == ViewType.PROGRESS.id) {
            ProgressItemViewHolder(parent)
        } else {
            EventItemViewHolder(parent, itemClickListener, rewindClickListener)
        }
    }
}
