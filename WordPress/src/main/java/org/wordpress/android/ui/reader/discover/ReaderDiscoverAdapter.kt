package org.wordpress.android.ui.reader.discover

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderViewHolder.ReaderPostViewHolder

private const val postViewType: Int = 1

class ReaderDiscoverAdapter : Adapter<ReaderViewHolder>() {
    private val items = mutableListOf<ReaderCardUiState>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReaderViewHolder {
        return when (viewType) {
            postViewType -> ReaderPostViewHolder(parent)
            else -> throw NotImplementedError("Unknown ViewType")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ReaderViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    fun update(newItems: List<ReaderCardUiState>) {
        val diffResult = DiffUtil.calculateDiff(DiscoverDiffUtil(items, newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ReaderPostUiState -> postViewType
        }
    }

    private class DiscoverDiffUtil(
        val oldItems: List<ReaderCardUiState>,
        val newItems: List<ReaderCardUiState>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            if (oldItem::class != newItem::class) {
                return false
            }
            return when (oldItem) {
                is ReaderPostUiState -> {
                    oldItem.postId == (newItem as ReaderPostUiState).postId && oldItem.blogId == newItem.blogId
                }
            }
        }

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
