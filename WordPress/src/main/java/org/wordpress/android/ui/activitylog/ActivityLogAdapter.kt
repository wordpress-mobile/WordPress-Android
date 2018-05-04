package org.wordpress.android.ui.activitylog

import android.content.Context
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.LayoutInflater
import android.view.ViewGroup
import org.wordpress.android.R.layout
import org.wordpress.android.viewmodel.activitylog.ActivityLogListItemViewModel
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel

class ActivityLogAdapter(context: Context, private val viewModel: ActivityLogViewModel) : Adapter<ActivityLogViewHolder>() {
    private val list = mutableListOf<ActivityLogListItemViewModel>()
    private var layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onBindViewHolder(holder: ActivityLogViewHolder, position: Int) {
        holder.bind(getItem(position)!!, getItem(position - 1))

        if (position == itemCount - 1) {
            viewModel.loadMore()
        }
    }

    init {
        setHasStableIds(true)
    }

    internal fun updateList(items: List<ActivityLogListItemViewModel>) {
        val diffResult = DiffUtil.calculateDiff(ActivityLogDiffCallback(list, items))
        list.clear()
        list.addAll(items)

        diffResult.dispatchUpdatesTo(this)
    }

    private fun getItem(position: Int): ActivityLogListItemViewModel? {
        return if (position < 0) null else list[position]
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemId(position: Int): Long {
        return list[position].activityId.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityLogViewHolder {
        val view = layoutInflater.inflate(layout.activity_log_list_item, parent, false) as ViewGroup
        return ActivityLogViewHolder(view)
    }
}
