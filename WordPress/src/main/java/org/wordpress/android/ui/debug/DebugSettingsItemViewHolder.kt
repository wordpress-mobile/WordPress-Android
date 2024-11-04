package org.wordpress.android.ui.debug

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.databinding.DebugSettingsFeatureBinding
import org.wordpress.android.databinding.DebugSettingsRemoteFieldBinding

sealed class DebugSettingsItemViewHolder(
    parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    class RemoteFeatureFlagViewHolder(parent: ViewGroup) :
        DebugSettingsItemViewHolder(parent, R.layout.debug_settings_feature) {
        fun bind(item: UiItem.FeatureFlag.RemoteFeatureFlag) {
            with(DebugSettingsFeatureBinding.bind(itemView)) {
                showFeatureFlag(item)
                remoteFieldSource.text = item.source
                remoteFieldSource.visibility = View.VISIBLE
            }
        }
    }

    class LocalFeatureFlagViewHolder(parent: ViewGroup) :
        DebugSettingsItemViewHolder(parent, R.layout.debug_settings_feature) {
        fun bind(item: UiItem.FeatureFlag.LocalFeatureFlag) {
            with(DebugSettingsFeatureBinding.bind(itemView)) {
                showFeatureFlag(item)
                remoteFieldSource.visibility = View.GONE
            }
        }
    }

    fun DebugSettingsFeatureBinding.showFeatureFlag(item: UiItem.FeatureFlag) {
        featureTitle.text = item.title
        when (item.state) {
            UiItem.FeatureFlag.State.ENABLED -> {
                featureEnabled.isVisible = true
                featureEnabled.isChecked = true
                unknownIcon.isVisible = false
            }

            UiItem.FeatureFlag.State.DISABLED -> {
                featureEnabled.isVisible = true
                featureEnabled.isChecked = false
                unknownIcon.isVisible = false
            }

            UiItem.FeatureFlag.State.UNKNOWN -> {
                // features in development are always unknown until the user taps on them
                if (item.type == DebugSettingsType.FEATURES_IN_DEVELOPMENT) {
                    featureEnabled.isVisible = true
                    featureEnabled.isChecked = (item as? UiItem.FeatureFlag.LocalFeatureFlag)?.enabled == true
                    unknownIcon.isVisible = false
                } else {
                    featureEnabled.isVisible = false
                    unknownIcon.isVisible = true
                }
            }
        }
        featureEnabled.setOnCheckedChangeListener { _, _ -> item.toggleAction.toggle() }
        itemView.setOnClickListener { item.toggleAction.toggle() }
        previewIcon.isVisible = item.preview != null
        previewIcon.setOnClickListener { item.preview?.invoke() }
    }

    class RemoteFieldConfigViewHolder(parent: ViewGroup) : DebugSettingsItemViewHolder(
        parent,
        R.layout.debug_settings_remote_field
    ) {
        fun bind(item: UiItem.Field) = with(DebugSettingsRemoteFieldBinding.bind(itemView)) {
            remoteFieldKey.text = item.remoteFieldKey
            remoteFieldValue.text = item.remoteFieldValue
            remoteFieldSource.text = item.remoteFieldSource
        }
    }
}
