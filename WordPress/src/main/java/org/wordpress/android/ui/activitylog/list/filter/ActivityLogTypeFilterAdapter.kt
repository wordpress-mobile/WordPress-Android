package org.wordpress.android.ui.activitylog.list.filter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState.ActivityType
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState.SectionHeader
import org.wordpress.android.ui.utils.UiHelpers

private const val headerViewType: Int = 1
private const val activityViewType: Int = 2

class ActivityLogTypeFilterAdapter(private val uiHelpers: UiHelpers) : Adapter<ActivityLogTypeFilterViewHolder>() {
    private val items = mutableListOf<ListItemUiState>()

    @Suppress("UseCheckOrError")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityLogTypeFilterViewHolder {
        return when (viewType) {
            headerViewType -> ActivityLogTypeFilterViewHolder.HeaderViewHolder(parent, uiHelpers)
            activityViewType -> ActivityLogTypeFilterViewHolder.ActivityTypeViewHolder(parent, uiHelpers)
            else -> throw IllegalStateException("View type ($viewType) not implemented")
        }
    }

    override fun onBindViewHolder(holder: ActivityLogTypeFilterViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SectionHeader -> headerViewType
            is ActivityType -> activityViewType
        }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<ListItemUiState>) {
        val diffResult = DiffUtil.calculateDiff(TypeFilterDiffUtil(items, newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    private class TypeFilterDiffUtil(
        val oldItems: List<ListItemUiState>,
        val newItems: List<ListItemUiState>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            if (oldItem::class != newItem::class) {
                return false
            }
            return when (oldItem) {
                is SectionHeader -> true
                is ActivityType -> oldItem.title == (newItem as ActivityType).title
            }
        }

        override fun getOldListSize(): Int {
            return oldItems.size
        }

        override fun getNewListSize(): Int {
            return newItems.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
