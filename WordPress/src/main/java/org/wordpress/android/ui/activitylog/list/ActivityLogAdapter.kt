package org.wordpress.android.ui.activitylog.list

import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Event
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Header
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Notice
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Progress
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.ViewType
import org.wordpress.android.ui.utils.UiHelpers

class ActivityLogAdapter(
    private val itemClickListener: (ActivityLogListItem) -> Unit,
    private val secondaryActionClickListener: (ActivityLogListItem.SecondaryAction, ActivityLogListItem) -> Boolean,
    private val uiHelpers: UiHelpers
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
        when (holder) {
            is EventItemViewHolder -> holder.bind(list[position] as Event)
            is ProgressItemViewHolder -> holder.bind(list[position] as Progress)
            is HeaderItemViewHolder -> holder.bind(list[position] as Header)
            is FooterItemViewHolder -> {}
            is LoadingItemViewHolder -> {}
            is NoticeItemViewHolder -> holder.bind(list[position] as Notice, uiHelpers)
            else -> throw IllegalArgumentException("Unexpected view holder in ActivityLog")
        }
    }

    init {
        setHasStableIds(true)
    }

    internal fun updateList(items: List<ActivityLogListItem>) {
        val diffResult = DiffUtil.calculateDiff(ActivityLogDiffCallback(list.toList(), items))
        list.clear()
        list.addAll(items)

        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemId(position: Int): Long = list[position].longId()

    override fun getItemViewType(position: Int): Int = list[position].type.id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityLogViewHolder {
        return when (viewType) {
            ViewType.PROGRESS.id -> ProgressItemViewHolder(parent)
            ViewType.EVENT.id -> EventItemViewHolder(parent, itemClickListener, secondaryActionClickListener)
            ViewType.HEADER.id -> HeaderItemViewHolder(parent)
            ViewType.FOOTER.id -> FooterItemViewHolder(parent)
            ViewType.LOADING.id -> LoadingItemViewHolder(parent)
            ViewType.NOTICE.id -> NoticeItemViewHolder(parent)
            else -> throw IllegalArgumentException("Unexpected view type in ActivityLog")
        }
    }
}
