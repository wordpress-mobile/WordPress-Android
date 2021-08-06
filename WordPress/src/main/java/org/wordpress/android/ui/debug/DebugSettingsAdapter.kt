package org.wordpress.android.ui.debug

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.debug.DebugSettingsItemViewHolder.ButtonViewHolder
import org.wordpress.android.ui.debug.DebugSettingsItemViewHolder.FeatureViewHolder
import org.wordpress.android.ui.debug.DebugSettingsItemViewHolder.HeaderViewHolder
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Button
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Header
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.BUTTON
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.FEATURE
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.HEADER

class DebugSettingsAdapter : Adapter<DebugSettingsItemViewHolder>() {
    private var items: List<UiItem> = listOf()

    fun update(newItems: List<UiItem>) {
        val diffResult = DiffUtil.calculateDiff(
                DebugSettingsDiffCallback(
                        items,
                        newItems
                )
        )
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebugSettingsItemViewHolder {
        return when (Type.values()[viewType]) {
            HEADER -> HeaderViewHolder(parent)
            FEATURE -> FeatureViewHolder(parent)
            BUTTON -> ButtonViewHolder(parent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: DebugSettingsItemViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(items[position] as Header)
            is FeatureViewHolder -> holder.bind(items[position] as Feature)
            is ButtonViewHolder -> holder.bind(items[position] as Button)
        }
    }
}
