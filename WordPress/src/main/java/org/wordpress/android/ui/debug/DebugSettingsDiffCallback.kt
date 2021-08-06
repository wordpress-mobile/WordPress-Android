package org.wordpress.android.ui.debug

import androidx.recyclerview.widget.DiffUtil.Callback
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem.Feature
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem.Header

class DebugSettingsDiffCallback(
    private val oldList: List<FeatureUiItem>,
    private val newList: List<FeatureUiItem>
) : Callback() {
    object Payload
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return oldItem.type == newItem.type && when {
            oldItem is Header && newItem is Header -> oldItem.header == newItem.header
            oldItem is Feature && newItem is Feature -> oldItem.title == newItem.title
            else -> true
        }
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
