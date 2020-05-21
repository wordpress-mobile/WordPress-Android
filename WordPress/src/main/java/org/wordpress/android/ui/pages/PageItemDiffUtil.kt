package org.wordpress.android.ui.pages

import androidx.recyclerview.widget.DiffUtil.Callback
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.ParentPage
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState.Determinate

class PageItemDiffUtil(val items: List<PageItem>, val result: List<PageItem>) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = items[oldItemPosition]
        val newItem = result[newItemPosition]
        return oldItem.type == newItem.type && when (oldItem) {
            is Page -> oldItem.remoteId == (newItem as Page).remoteId
            is ParentPage -> oldItem.id == (newItem as ParentPage).id
            else -> oldItem == newItem
        }
    }

    override fun getOldListSize(): Int = items.size

    override fun getNewListSize(): Int = result.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return items[oldItemPosition] == result[newItemPosition]
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = items[oldItemPosition]
        val newItem = result[newItemPosition]

        if (oldItem is Page && newItem is Page) {
            /**
             * Suppresses the default animation if the progress has changed to prevent blinking as the upload progresses
             *
             * We don't need to use the payload in onBindViewHolder unless we want to. Passing a non-null value
             * suppresses the default ItemAnimator, which is all we need in this case.
             */
            if (oldItem.progressBarUiState is Determinate &&
                    newItem.progressBarUiState is Determinate &&
                    (oldItem.progressBarUiState as Determinate).progress !=
                    (newItem.progressBarUiState as Determinate).progress) {
                return true
            }
        }
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }
}
