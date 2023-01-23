package org.wordpress.android.ui.activitylog.list.filter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState.ActivityType
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState.SectionHeader
import org.wordpress.android.ui.utils.UiHelpers

sealed class ActivityLogTypeFilterViewHolder(internal val parent: ViewGroup, @LayoutRes layout: Int) :
    RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(uiState: ListItemUiState)

    class ActivityTypeViewHolder(
        parent: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : ActivityLogTypeFilterViewHolder(parent, R.layout.activity_log_type_filter_item) {
        private val container = itemView.findViewById<ViewGroup>(R.id.container)
        private val activityType = itemView.findViewById<TextView>(R.id.activity_type)
        private val checkbox = itemView.findViewById<CheckBox>(R.id.checkbox)

        override fun onBind(uiState: ListItemUiState) {
            uiState as ActivityType
            uiHelpers.setTextOrHide(activityType, uiState.title)
            checkbox.isChecked = uiState.checked
            container.setOnClickListener {
                uiState.onClick.invoke()
            }
        }
    }

    class HeaderViewHolder(
        parent: ViewGroup,
        private val uiHelpers: UiHelpers
    ) : ActivityLogTypeFilterViewHolder(parent, R.layout.activity_log_type_filter_header) {
        private val headerTitle = itemView.findViewById<TextView>(R.id.header_title)
        override fun onBind(uiState: ListItemUiState) {
            uiState as SectionHeader
            uiHelpers.setTextOrHide(headerTitle, uiState.title)
        }
    }
}
