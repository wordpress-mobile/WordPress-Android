package org.wordpress.android.ui

import android.support.v7.util.DiffUtil
import org.wordpress.android.fluxc.model.list.PagedListItemType
import org.wordpress.android.fluxc.model.list.PagedListItemType.EndListIndicatorItem
import org.wordpress.android.fluxc.model.list.PagedListItemType.LoadingItem
import org.wordpress.android.fluxc.model.list.PagedListItemType.ReadyItem

class PagedListDiffItemCallback<T>(
    val getRemoteItemId: (T) -> Long?,
    val areItemsTheSame: (T, T) -> Boolean,
    val areContentsTheSame: (T, T) -> Boolean
) : DiffUtil.ItemCallback<PagedListItemType<T>>() {
    override fun areItemsTheSame(
        oldItem: PagedListItemType<T>,
        newItem: PagedListItemType<T>
    ): Boolean {
        if (oldItem is EndListIndicatorItem && newItem is EndListIndicatorItem) {
            return true
        }
        if (oldItem is LoadingItem && newItem is LoadingItem) {
            return oldItem.remoteItemId == newItem.remoteItemId
        }
        if (oldItem is ReadyItem && newItem is ReadyItem) {
            return areItemsTheSame(oldItem.item, newItem.item)
        }
        if (oldItem is LoadingItem && newItem is ReadyItem) {
            return oldItem.remoteItemId == getRemoteItemId(newItem.item)
        }
        return false
    }

    override fun areContentsTheSame(
        oldItem: PagedListItemType<T>,
        newItem: PagedListItemType<T>
    ): Boolean {
        if (oldItem is EndListIndicatorItem && newItem is EndListIndicatorItem) {
            return true
        }
        if (oldItem is LoadingItem && newItem is LoadingItem) {
            return true
        }
        if (oldItem is ReadyItem && newItem is ReadyItem) {
            return areContentsTheSame(oldItem.item, newItem.item)
        }
        return false
    }
}
