package org.wordpress.android.ui.activitylog

import android.content.Context
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wordpress.android.R.layout
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.viewmodel.activitylog.ActivityLogListItemViewModel
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel

class ActivityLogAdapter(context: Context, private val viewModel: ActivityLogViewModel) : Adapter<ActivityLogViewHolder>() {
    private val list = mutableListOf<ActivityLogListItemViewModel>()
    private var layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onBindViewHolder(holder: ActivityLogViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)

        if (position == itemCount - 1) {
            viewModel.loadMore()
        }

        holder.header.visibility = if (shouldDisplayHeader(position)) View.VISIBLE else View.GONE
        holder.button.visibility = if (item.rewindable == true) View.VISIBLE else View.GONE
    }

    init {
        setHasStableIds(true)
    }

    internal fun updateList(items: List<ActivityLogModel>) {
        val itemViewModels = items.map { ActivityLogListItemViewModel.fromDomainModel(it) }
        val diffResult = DiffUtil.calculateDiff(ActivityLogDiffCallback(list, itemViewModels))
        list.clear()
        list.addAll(itemViewModels)

        diffResult.dispatchUpdatesTo(this)
    }

    private fun shouldDisplayHeader(position: Int): Boolean {
        return if (position > 0) {
            list[position].header != list[position - 1].header
        } else {
            true
        }
    }

    private fun getItem(position: Int): ActivityLogListItemViewModel {
        return list[position]
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemId(position: Int): Long {
        return list[position].activityID.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityLogViewHolder {
        val view = layoutInflater.inflate(layout.activity_log_list_item, parent, false) as ViewGroup
        return ActivityLogViewHolder(view)
    }
}
