package org.wordpress.android.ui.reader.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.ui.reader.discover.viewholders.ReaderRelatedPostViewHolder
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState.RelatedPostsUiState.ReaderRelatedPostUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

class ReaderRelatedPostsAdapter(
    private val uiHelpers: UiHelpers,
    private val imageManager: ImageManager
) : Adapter<ReaderRelatedPostViewHolder>() {
    private val items = mutableListOf<ReaderRelatedPostUiState>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ReaderRelatedPostViewHolder(
        uiHelpers,
        imageManager,
        parent
    )

    override fun onBindViewHolder(holder: ReaderRelatedPostViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<ReaderRelatedPostUiState>) {
        val diffResult = DiffUtil.calculateDiff(RelatedPostsDiffUtil(items, newItems))
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    private class RelatedPostsDiffUtil(
        val oldItems: List<ReaderRelatedPostUiState>,
        val newItems: List<ReaderRelatedPostUiState>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            if (oldItem::class != newItem::class) {
                return false
            }
            return oldItem.postId == newItem.postId && oldItem.blogId == newItem.blogId
        }

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
