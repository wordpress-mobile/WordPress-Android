package org.wordpress.android.ui.debug

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.debug.FeatureItemViewHolder.ButtonViewHolder
import org.wordpress.android.ui.debug.FeatureItemViewHolder.FeatureViewHolder
import org.wordpress.android.ui.debug.FeatureItemViewHolder.HeaderViewHolder
import org.wordpress.android.ui.debug.ManualFeatureConfigViewModel.FeatureUiItem
import org.wordpress.android.ui.debug.ManualFeatureConfigViewModel.FeatureUiItem.Button
import org.wordpress.android.ui.debug.ManualFeatureConfigViewModel.FeatureUiItem.Feature
import org.wordpress.android.ui.debug.ManualFeatureConfigViewModel.FeatureUiItem.Header
import org.wordpress.android.ui.debug.ManualFeatureConfigViewModel.FeatureUiItem.Type
import org.wordpress.android.ui.debug.ManualFeatureConfigViewModel.FeatureUiItem.Type.BUTTON
import org.wordpress.android.ui.debug.ManualFeatureConfigViewModel.FeatureUiItem.Type.FEATURE
import org.wordpress.android.ui.debug.ManualFeatureConfigViewModel.FeatureUiItem.Type.HEADER

class FeatureAdapter : Adapter<FeatureItemViewHolder>() {
    private var items: List<FeatureUiItem> = listOf()

    fun update(newItems: List<FeatureUiItem>) {
        val diffResult = DiffUtil.calculateDiff(
                FeatureDiffCallback(
                        items,
                        newItems
                )
        )
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureItemViewHolder {
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

    override fun onBindViewHolder(holder: FeatureItemViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(items[position] as Header)
            is FeatureViewHolder -> holder.bind(items[position] as Feature)
            is ButtonViewHolder -> holder.bind(items[position] as Button)
        }
    }
}
