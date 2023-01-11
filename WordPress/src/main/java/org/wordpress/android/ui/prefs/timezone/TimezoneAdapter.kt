package org.wordpress.android.ui.prefs.timezone

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.ui.prefs.timezone.TimezonesList.TimezoneHeader
import org.wordpress.android.ui.prefs.timezone.TimezonesList.TimezoneItem

class TimezoneAdapter(
    private val onClick: (timezone: TimezoneItem) -> Unit
) : ListAdapter<TimezonesList, ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            R.layout.site_settings_timezone_bottom_sheet_list_header -> TimezoneHeaderViewHolder.from(parent)
            else -> TimezoneViewHolder.from(parent)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is TimezoneHeaderViewHolder -> holder.bind(item as TimezoneHeader)
            is TimezoneViewHolder -> holder.bind(item as TimezoneItem, onClick)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TimezoneHeader -> R.layout.site_settings_timezone_bottom_sheet_list_header
            else -> R.layout.site_settings_timezone_bottom_sheet_list_item
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TimezonesList>() {
            override fun areItemsTheSame(oldItem: TimezonesList, newItem: TimezonesList) =
                oldItem.label == newItem.label

            override fun areContentsTheSame(oldItem: TimezonesList, newItem: TimezonesList) =
                oldItem == newItem
        }
    }
}
