package org.wordpress.android.ui.debug

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import org.wordpress.android.ui.debug.DebugSettingsItemViewHolder.ButtonViewHolder
import org.wordpress.android.ui.debug.DebugSettingsItemViewHolder.FeatureViewHolder
import org.wordpress.android.ui.debug.DebugSettingsItemViewHolder.HeaderViewHolder
import org.wordpress.android.ui.debug.DebugSettingsItemViewHolder.RemoteFieldConfigViewHolder
import org.wordpress.android.ui.debug.DebugSettingsItemViewHolder.RowViewHolder
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Button
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Field
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Header
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Row
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.BUTTON
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.FEATURE
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.FIELD
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.HEADER
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.ROW

class DebugSettingsAdapter : ListAdapter<UiItem, DebugSettingsItemViewHolder>(DebugSettingsDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebugSettingsItemViewHolder {
        return when (Type.values()[viewType]) {
            HEADER -> HeaderViewHolder(parent)
            FEATURE -> FeatureViewHolder(parent)
            BUTTON -> ButtonViewHolder(parent)
            ROW -> RowViewHolder(parent)
            FIELD -> RemoteFieldConfigViewHolder(parent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type.ordinal
    }

    override fun onBindViewHolder(holder: DebugSettingsItemViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(getItem(position) as Header)
            is FeatureViewHolder -> holder.bind(getItem(position) as Feature)
            is ButtonViewHolder -> holder.bind(getItem(position) as Button)
            is RowViewHolder -> holder.bind(getItem(position) as Row)
            is RemoteFieldConfigViewHolder -> holder.bind(getItem(position) as Field)
        }
    }
}
