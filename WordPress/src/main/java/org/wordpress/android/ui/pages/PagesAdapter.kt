package org.wordpress.android.ui.pages

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.ParentPage
import org.wordpress.android.ui.pages.PageItemViewHolder.EmptyViewHolder
import org.wordpress.android.ui.pages.PageItemViewHolder.PageDividerViewHolder
import org.wordpress.android.ui.pages.PageItemViewHolder.PageParentViewHolder
import org.wordpress.android.ui.pages.PageItemViewHolder.PageViewHolder

class PagesAdapter(
    private val onMenuAction: (PageItem.Action, Page) -> Boolean = { _, _ -> false },
    private val onItemTapped: (Page) -> Unit = { },
    private val onEmptyActionButtonTapped: () -> Unit = { },
    private val onParentSelected: (ParentPage) -> Unit = { },
    private val uiScope: CoroutineScope
) : Adapter<PageItemViewHolder>() {
    private val items = mutableListOf<PageItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageItemViewHolder {
        return when (viewType) {
            PageItem.Type.PAGE.viewType -> PageViewHolder(parent, onMenuAction, onItemTapped)
            PageItem.Type.DIVIDER.viewType -> PageDividerViewHolder(parent)
            PageItem.Type.EMPTY.viewType -> EmptyViewHolder(parent, onEmptyActionButtonTapped)
            PageItem.Type.PARENT.viewType -> PageParentViewHolder(parent,
                    this::selectParent,
                    R.layout.page_parent_list_item)
            PageItem.Type.TOP_LEVEL_PARENT.viewType -> PageParentViewHolder(parent,
                    this::selectParent,
                    R.layout.page_parent_top_level_item)
            else -> throw Throwable("Unexpected view type")
        }
    }

    private fun selectParent(parent: ParentPage) {
        onParentSelected(parent)
        uiScope.launch {
            delay(200) // let the selection animation play out before refreshing the list
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return items[position].type.viewType
    }

    override fun onBindViewHolder(holder: PageItemViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    fun update(result: List<PageItem>) {
        val diffResult = DiffUtil.calculateDiff(PageItemDiffUtil(items.toList(), result))
        items.clear()
        items.addAll(result)
        diffResult.dispatchUpdatesTo(this)
    }

    private class PageItemDiffUtil(val items: List<PageItem>, val result: List<PageItem>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = result[newItemPosition]
            return oldItem.type == newItem.type && when (oldItem) {
                is Page -> oldItem.id == (newItem as Page).id
                is ParentPage -> oldItem.id == (newItem as ParentPage).id
                else -> oldItem == newItem
            }
        }

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = result.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return items[oldItemPosition] == result[newItemPosition]
        }
    }
}
