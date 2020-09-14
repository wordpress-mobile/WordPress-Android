package org.wordpress.android.util.config.setup

import androidx.recyclerview.widget.DiffUtil.Callback
import org.wordpress.android.util.config.setup.ManualFeatureConfigViewModel.FeatureUiItem
import org.wordpress.android.util.config.setup.ManualFeatureConfigViewModel.FeatureUiItem.Header

class FeatureBlockDiffCallback(
    private val oldList: List<FeatureUiItem>,
    private val newList: List<FeatureUiItem>
) : Callback() {
    object Payload
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return oldItem.type == newItem.type && when (oldItem) {
            is Header -> oldItem.header == (newItem as Header).header
            is FeatureUiItem.Feature -> oldItem.title == (newItem as FeatureUiItem.Feature).title
        }
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
