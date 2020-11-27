package org.wordpress.android.ui.activitylog.list.filter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState.ActivityType
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState.SectionHeader
import org.wordpress.android.ui.utils.UiHelpers

private const val headerViewType: Int = 1
private const val activityViewType: Int = 2

class ActivityLogTypeFilterAdapter(private val uiHelpers: UiHelpers) : Adapter<ActivityLogTypeFilterViewHolder>() {
    private val items = mutableListOf<ListItemUiState>()

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
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
