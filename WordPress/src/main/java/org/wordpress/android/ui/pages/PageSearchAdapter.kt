package org.wordpress.android.ui.pages

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItemViewHolder.EmptyViewHolder
import org.wordpress.android.ui.pages.PageItemViewHolder.PageDividerViewHolder
import org.wordpress.android.ui.pages.PageItemViewHolder.PageViewHolder

class PageSearchAdapter(
    private val onMenuAction: (PageItem.Action, Page) -> Boolean,
    private val onItemTapped: (Page) -> Unit
) : Adapter<PageItemViewHolder>() {
    private val items = mutableListOf<PageItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageItemViewHolder {
        return when (viewType) {
            PageItem.Type.PAGE.viewType -> PageViewHolder(parent, onMenuAction, onItemTapped)
            PageItem.Type.DIVIDER.viewType -> PageDividerViewHolder(parent)
            PageItem.Type.EMPTY.viewType -> EmptyViewHolder(parent) { }
            else -> throw Throwable("Unexpected view type")
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
}
