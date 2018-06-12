package org.wordpress.android.ui.activitylog.list

import android.content.Context
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.LayoutInflater
import android.view.ViewGroup
import org.wordpress.android.R.layout
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel

class ActivityLogAdapter(
    context: Context,
    private val viewModel: ActivityLogViewModel,
    private val itemClickListener: (ActivityLogListItem) -> Unit,
    private val rewindClickListener: (ActivityLogListItem) -> Unit
) : Adapter<ActivityLogViewHolder>() {
    private val list = mutableListOf<ActivityLogListItem>()
    private var layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onBindViewHolder(holder: ActivityLogViewHolder, position: Int) {
        getItem(position)?.let { current ->
            holder.bind(current, getItem(position - 1), getItem(position + 1))

            if (position == itemCount - 1) {
                viewModel.loadMore()
            }
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
        val view = layoutInflater.inflate(layout.activity_log_list_item, parent, false) as ViewGroup
        return ActivityLogViewHolder(view, itemClickListener, rewindClickListener)
    }
}
