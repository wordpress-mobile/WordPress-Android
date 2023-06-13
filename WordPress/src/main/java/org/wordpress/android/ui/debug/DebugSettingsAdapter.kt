package org.wordpress.android.ui.debug

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import org.wordpress.android.ui.debug.DebugSettingsItemViewHolder.FeatureViewHolder
import org.wordpress.android.ui.debug.DebugSettingsItemViewHolder.RemoteFieldConfigViewHolder
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Field
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.FEATURE
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.FIELD

class DebugSettingsAdapter : ListAdapter<UiItem, DebugSettingsItemViewHolder>(DebugSettingsDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebugSettingsItemViewHolder {
        return when (Type.values()[viewType]) {
            FEATURE -> FeatureViewHolder(parent)
            FIELD -> RemoteFieldConfigViewHolder(parent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type.ordinal
    }

    override fun onBindViewHolder(holder: DebugSettingsItemViewHolder, position: Int) {
        when (holder) {
            is FeatureViewHolder -> holder.bind(getItem(position) as Feature)
            is RemoteFieldConfigViewHolder -> holder.bind(getItem(position) as Field)
        }
    }
}
