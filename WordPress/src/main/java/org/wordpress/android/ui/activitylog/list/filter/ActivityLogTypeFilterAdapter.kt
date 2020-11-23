package org.wordpress.android.ui.activitylog.list.filter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ItemUiState
import org.wordpress.android.ui.utils.UiHelpers

class ActivityLogTypeFilterAdapter(private val uiHelpers: UiHelpers) : Adapter<ActivityLogTypeFilterViewHolder>() {
    private val items = mutableListOf<ItemUiState>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityLogTypeFilterViewHolder {
        return ActivityLogTypeFilterViewHolder(parent, uiHelpers)
    }

    override fun onBindViewHolder(holder: ActivityLogTypeFilterViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<ItemUiState>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
