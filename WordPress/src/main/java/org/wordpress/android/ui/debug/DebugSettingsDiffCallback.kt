package org.wordpress.android.ui.debug

import androidx.recyclerview.widget.DiffUtil
import org.wordpress.android.ui.debug.UiItem.FeatureFlag.RemoteFeatureFlag
import org.wordpress.android.ui.debug.UiItem.FeatureFlag.LocalFeatureFlag

class DebugSettingsDiffCallback : DiffUtil.ItemCallback<UiItem>() {
    override fun areItemsTheSame(oldItem: UiItem, newItem: UiItem): Boolean {
        return oldItem.type == newItem.type && when {
            oldItem is RemoteFeatureFlag && newItem is RemoteFeatureFlag -> oldItem == newItem
            oldItem is LocalFeatureFlag && newItem is LocalFeatureFlag -> oldItem == newItem
            oldItem is UiItem.Field && newItem is UiItem.Field -> oldItem == newItem
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: UiItem, newItem: UiItem): Boolean {
        return oldItem == newItem
    }
}
