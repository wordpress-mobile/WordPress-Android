package org.wordpress.android.ui.reader.discover

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.viewholders.ReaderPostViewHolder
import org.wordpress.android.ui.reader.discover.viewholders.ReaderViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

private const val postViewType: Int = 1
private const val interestViewType: Int = 2

class ReaderDiscoverAdapter(
    private val uiHelpers: UiHelpers,
    private val imageManager: ImageManager
) : Adapter<ReaderViewHolder>() {
    private val items = mutableListOf<ReaderCardUiState>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReaderViewHolder {
        return when (viewType) {
            postViewType -> ReaderPostViewHolder(
                    uiHelpers,
                    imageManager,
                    parent
            )
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
            is ReaderInterestUiState -> interestViewType
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

                is ReaderInterestUiState -> {
                    oldItem == newItem
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
