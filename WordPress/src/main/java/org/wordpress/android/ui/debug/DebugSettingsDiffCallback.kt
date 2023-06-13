package org.wordpress.android.ui.debug

import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature

class DebugSettingsDiffCallback : DiffUtil.ItemCallback<UiItem>() {
    override fun areItemsTheSame(oldItem: UiItem, newItem: UiItem): Boolean {
        return oldItem.type == newItem.type && when {
            oldItem is Feature && newItem is Feature -> oldItem == newItem
            oldItem is UiItem.Field && newItem is UiItem.Field -> oldItem == newItem
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: UiItem, newItem: UiItem): Boolean {
        return oldItem == newItem
    }
}
