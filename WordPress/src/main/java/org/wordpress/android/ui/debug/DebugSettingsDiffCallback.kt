package org.wordpress.android.ui.debug

import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Header

class DebugSettingsDiffCallback : DiffUtil.ItemCallback<UiItem>() {
    override fun areItemsTheSame(oldItem: UiItem, newItem: UiItem): Boolean {
        return oldItem.type == newItem.type && when {
            oldItem is Header && newItem is Header -> oldItem.header == newItem.header
            oldItem is Feature && newItem is Feature -> oldItem.title == newItem.title
            else -> true
        }
    }

    override fun areContentsTheSame(oldItem: UiItem, newItem: UiItem): Boolean {
        return oldItem == newItem
    }
}
