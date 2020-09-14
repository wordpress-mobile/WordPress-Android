package org.wordpress.android.util.config.setup

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R
import org.wordpress.android.util.config.setup.ManualFeatureConfigViewModel.FeatureUiItem

sealed class FeatureItemViewHolder(
    parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    class HeaderViewHolder(parent: ViewGroup) : FeatureItemViewHolder(parent, R.layout.manual_feature_header) {
        private val header = itemView.findViewById<TextView>(R.id.header)
        fun bind(item: FeatureUiItem.Header) {
            header.setText(item.header)
        }
    }

    class FeatureViewHolder(parent: ViewGroup) : FeatureItemViewHolder(parent, R.layout.manual_feature_item) {
        private val title = itemView.findViewById<TextView>(R.id.feature_title)
        private val enabled = itemView.findViewById<CheckBox>(R.id.feature_enabled)
        fun bind(item: FeatureUiItem.Feature) {
            title.text = item.title
            enabled.isChecked = item.enabled
            enabled.setOnCheckedChangeListener { _, _ -> item.toggleAction() }
        }
    }
}
