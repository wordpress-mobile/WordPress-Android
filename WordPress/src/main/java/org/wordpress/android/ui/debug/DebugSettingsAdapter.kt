package org.wordpress.android.ui.debug

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import org.wordpress.android.ui.debug.DebugSettingsItemViewHolder.LocalFeatureFlagViewHolder
import org.wordpress.android.ui.debug.DebugSettingsItemViewHolder.RemoteFeatureFlagViewHolder
import org.wordpress.android.ui.debug.DebugSettingsItemViewHolder.RemoteFieldConfigViewHolder
import org.wordpress.android.ui.debug.DebugSettingsType.FEATURES_IN_DEVELOPMENT
import org.wordpress.android.ui.debug.DebugSettingsType.REMOTE_FEATURES
import org.wordpress.android.ui.debug.DebugSettingsType.REMOTE_FIELD_CONFIGS
import org.wordpress.android.ui.debug.UiItem.FeatureFlag.LocalFeatureFlag
import org.wordpress.android.ui.debug.UiItem.FeatureFlag.RemoteFeatureFlag
import org.wordpress.android.ui.debug.UiItem.Field

class DebugSettingsAdapter : ListAdapter<UiItem, DebugSettingsItemViewHolder>(DebugSettingsDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebugSettingsItemViewHolder {
        return when (DebugSettingsType.values()[viewType]) {
            REMOTE_FEATURES -> RemoteFeatureFlagViewHolder(parent)
            FEATURES_IN_DEVELOPMENT -> LocalFeatureFlagViewHolder(parent)
            REMOTE_FIELD_CONFIGS -> RemoteFieldConfigViewHolder(parent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type.ordinal
    }

    override fun onBindViewHolder(holder: DebugSettingsItemViewHolder, position: Int) {
        when (holder) {
            is RemoteFeatureFlagViewHolder -> holder.bind(getItem(position) as RemoteFeatureFlag)
            is LocalFeatureFlagViewHolder -> holder.bind(getItem(position) as LocalFeatureFlag)
            is RemoteFieldConfigViewHolder -> holder.bind(getItem(position) as Field)
        }
    }
}
