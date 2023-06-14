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
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature.State.DISABLED
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature.State.ENABLED
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature.State.UNKNOWN

sealed class DebugSettingsItemViewHolder(
    parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    class FeatureViewHolder(parent: ViewGroup) : DebugSettingsItemViewHolder(parent, R.layout.debug_settings_feature) {
        fun bind(item: UiItem.Feature) = with(DebugSettingsFeatureBinding.bind(itemView)) {
            featureTitle.text = item.title
            featureEnabled.visibility = View.GONE
            unknownIcon.visibility = View.GONE
            featureEnabled.setOnCheckedChangeListener(null)
            remoteFieldSource.text = item.source
            when (item.state) {
                ENABLED -> {
                    featureEnabled.visibility = View.VISIBLE
                    featureEnabled.isChecked = true
                }
                DISABLED -> {
                    featureEnabled.visibility = View.VISIBLE
                    featureEnabled.isChecked = false
                }
                UNKNOWN -> {
                    unknownIcon.visibility = View.VISIBLE
                }
            }
            featureEnabled.setOnCheckedChangeListener { _, _ -> item.toggleAction.toggle() }
            itemView.setOnClickListener { item.toggleAction.toggle() }
            previewIcon.isVisible = item.preview != null
            previewIcon.setOnClickListener { item.preview?.invoke() }
        }
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
