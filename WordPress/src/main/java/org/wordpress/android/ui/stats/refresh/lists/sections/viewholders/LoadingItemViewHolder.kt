package org.wordpress.android.ui.stats.refresh.lists.sections.viewholders

import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.LoadingItem

class LoadingItemViewHolder(parent: ViewGroup) : BlockListItemViewHolder(
    parent,
    R.layout.stats_block_loading_item
) {
    fun bind(loadingItem: LoadingItem) {
        if (!loadingItem.isLoading) {
            loadingItem.loadMore()
        }
    }
}
