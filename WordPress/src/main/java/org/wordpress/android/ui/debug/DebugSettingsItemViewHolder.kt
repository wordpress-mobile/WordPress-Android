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
        featureEnabled.visibility = View.GONE
        unknownIcon.visibility = View.GONE
        featureEnabled.setOnCheckedChangeListener(null)
        when (item.state) {
            UiItem.FeatureFlag.State.ENABLED -> {
                featureEnabled.visibility = View.VISIBLE
                featureEnabled.isChecked = true
            }

            UiItem.FeatureFlag.State.DISABLED -> {
                featureEnabled.visibility = View.VISIBLE
                featureEnabled.isChecked = false
            }

            UiItem.FeatureFlag.State.UNKNOWN -> {
                unknownIcon.visibility = View.VISIBLE
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
            remoteFieldKey.setText(item.remoteFieldKey)
            remoteFieldValue.setText(item.remoteFieldValue)
            remoteFieldSource.setText(item.remoteFieldSource)
        }
    }
}
