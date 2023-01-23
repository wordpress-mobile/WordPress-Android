package org.wordpress.android.ui.posts

import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.ui.posts.PrepublishingCategoriesViewHolder.PrepublishingCategoriesListItemViewHolder
import org.wordpress.android.ui.posts.PrepublishingCategoriesViewModel.PrepublishingCategoriesListItemUiState
import org.wordpress.android.ui.utils.UiHelpers

class PrepublishingCategoriesAdapter(private val uiHelpers: UiHelpers) :
    RecyclerView.Adapter<PrepublishingCategoriesViewHolder<*>>() {
    private val items = mutableListOf<PrepublishingCategoriesListItemUiState>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PrepublishingCategoriesViewHolder<*> {
        return PrepublishingCategoriesListItemViewHolder(parent, uiHelpers)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: PrepublishingCategoriesViewHolder<*>, position: Int) {
        holder.onBind(items[position], position)
    }

    @MainThread
    fun update(newItems: List<PrepublishingCategoriesListItemUiState>) {
        val diffResult = DiffUtil.calculateDiff(
            PrepublishingCategoriesDiffUtils(
                items.toList(),
                newItems
            )
        )
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    private class PrepublishingCategoriesDiffUtils(
        val oldItems: List<PrepublishingCategoriesListItemUiState>,
        val newItems: List<PrepublishingCategoriesListItemUiState>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            if (oldItem::class != newItem::class) {
                return false
            }

            return (oldItem).categoryNode.categoryId == (newItem).categoryNode.categoryId
        }

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
