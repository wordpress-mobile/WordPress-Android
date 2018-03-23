package org.wordpress.android.ui.activitylog

import android.arch.paging.PagedListAdapter
import android.support.v7.util.DiffUtil
import android.view.ViewGroup
import org.wordpress.android.fluxc.model.activity.ActivityLogModel

class ActivityLogAdapter : PagedListAdapter<ActivityLogModel, ActivityLogViewHolder>(diffCallback) {
    override fun onBindViewHolder(holder: ActivityLogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityLogViewHolder =
        ActivityLogViewHolder(parent)

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<ActivityLogModel>() {
            override fun areContentsTheSame(oldItem: ActivityLogModel, newItem: ActivityLogModel): Boolean =
                    oldItem.name == newItem.name && oldItem.summary == newItem.summary

            override fun areItemsTheSame(oldItem: ActivityLogModel, newItem: ActivityLogModel): Boolean =
                    oldItem.activityID == newItem.activityID
        }
    }
}
