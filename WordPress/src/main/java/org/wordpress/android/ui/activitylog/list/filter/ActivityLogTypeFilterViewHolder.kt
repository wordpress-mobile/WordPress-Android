package org.wordpress.android.ui.activitylog.list.filter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState
import org.wordpress.android.ui.utils.UiHelpers

class ActivityLogTypeFilterViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.activity_log_type_filter_item, parent, false)
) {
    fun onBind(uiState: ListItemUiState) {
    }
}
