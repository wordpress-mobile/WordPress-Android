package org.wordpress.android.ui.debug

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem.Feature.State.DISABLED
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem.Feature.State.ENABLED
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem.Feature.State.UNKNOWN

sealed class DebugSettingsItemViewHolder(
    parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    class HeaderViewHolder(parent: ViewGroup) : DebugSettingsItemViewHolder(parent, R.layout.manual_feature_header) {
        private val header = itemView.findViewById<TextView>(R.id.header)
        fun bind(item: FeatureUiItem.Header) {
            header.setText(item.header)
        }
    }

    class ButtonViewHolder(parent: ViewGroup) : DebugSettingsItemViewHolder(parent, R.layout.manual_feature_button) {
        private val button = itemView.findViewById<Button>(R.id.button)
        fun bind(item: FeatureUiItem.Button) {
            button.setText(item.text)
            button.setOnClickListener { item.clickAction() }
        }
    }

    class FeatureViewHolder(parent: ViewGroup) : DebugSettingsItemViewHolder(parent, R.layout.manual_feature_item) {
        private val title = itemView.findViewById<TextView>(R.id.feature_title)
        private val enabled = itemView.findViewById<CheckBox>(R.id.feature_enabled)
        private val unknown = itemView.findViewById<ImageView>(R.id.unknown_icon)
        fun bind(item: FeatureUiItem.Feature) {
            title.text = item.title
            enabled.visibility = View.GONE
            unknown.visibility = View.GONE
            enabled.setOnCheckedChangeListener(null)
            when (item.state) {
                ENABLED -> {
                    enabled.visibility = View.VISIBLE
                    enabled.isChecked = true
                }
                DISABLED -> {
                    enabled.visibility = View.VISIBLE
                    enabled.isChecked = false
                }
                UNKNOWN -> {
                    unknown.visibility = View.VISIBLE
                }
            }
            enabled.setOnCheckedChangeListener { _, _ -> item.toggleAction.toggle() }
            itemView.setOnClickListener { item.toggleAction.toggle() }
        }
    }
}
