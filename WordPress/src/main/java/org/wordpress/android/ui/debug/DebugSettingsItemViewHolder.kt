package org.wordpress.android.ui.debug

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.databinding.DebugSettingsRowBinding
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature.State.DISABLED
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature.State.ENABLED
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature.State.UNKNOWN

sealed class DebugSettingsItemViewHolder(
    parent: ViewGroup,
    @LayoutRes layout: Int
) : ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    class HeaderViewHolder(parent: ViewGroup) : DebugSettingsItemViewHolder(parent, R.layout.debug_settings_header) {
        private val header = itemView.findViewById<TextView>(R.id.header)
        fun bind(item: UiItem.Header) {
            header.setText(item.header)
        }
    }

    class ButtonViewHolder(parent: ViewGroup) : DebugSettingsItemViewHolder(parent, R.layout.debug_settings_button) {
        private val button = itemView.findViewById<Button>(R.id.button)
        fun bind(item: UiItem.Button) {
            button.setText(item.text)
            button.setOnClickListener { item.clickAction() }
        }
    }

    class FeatureViewHolder(val parent: ViewGroup) : DebugSettingsItemViewHolder(parent, R.layout.debug_settings_feature) {
        private val title = itemView.findViewById<TextView>(R.id.feature_title)
        private val enabled = itemView.findViewById<CheckBox>(R.id.feature_enabled)
        private val unknown = itemView.findViewById<ImageView>(R.id.unknown_icon)
        fun bind(item: UiItem.Feature) {
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
            // To facilitate testing, clear the Glide memory and disk caches every time the flag is toggled
            enabled.setOnCheckedChangeListener { _, _ ->
                item.toggleAction.toggle()
                clearGlideCache()
            }
            itemView.setOnClickListener {
                item.toggleAction.toggle()
                clearGlideCache()
            }
        }

        // Cheap hack for testing ;)
        fun clearGlideCache() {
            runBlocking {
                Glide.get(parent.context).clearMemory()
                Log.d("Glide", "memory cache cleared!")
                async(Dispatchers.IO) {
                    Glide.get(parent.context).clearDiskCache()
                    Log.d("Glide", "disk cache cleared!")
                }
            }
        }
    }

    class RowViewHolder(parent: ViewGroup) : DebugSettingsItemViewHolder(parent, R.layout.debug_settings_row) {
        fun bind(item: UiItem.Row) = with(DebugSettingsRowBinding.bind(itemView)) {
            title.setText(item.title)
            root.setOnClickListener { item.onClick.click() }
        }
    }
}
