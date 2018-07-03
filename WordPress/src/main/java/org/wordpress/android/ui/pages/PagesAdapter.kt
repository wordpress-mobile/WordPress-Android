package org.wordpress.android.ui.pages

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.wordpress.android.ui.pages.PageItemViewHolder.EmptyViewHolder
import org.wordpress.android.ui.pages.PageItemViewHolder.PageDividerViewHolder
import org.wordpress.android.ui.pages.PageItemViewHolder.PageViewHolder

class PagesAdapter(private val onAction: (PageItem.Action, PageItem) -> Boolean) :
        RecyclerView.Adapter<PageItemViewHolder>() {
    private val items = mutableListOf<PageItem>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageItemViewHolder {
        val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            PageItem.Type.PAGE.viewType -> PageViewHolder(layoutInflater, parent, onAction)
            PageItem.Type.DIVIDER.viewType -> PageDividerViewHolder(layoutInflater, parent)
            PageItem.Type.EMPTY.viewType -> EmptyViewHolder(layoutInflater, parent)
            else -> throw IllegalArgumentException("Unexpected view type")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return items[position].type.viewType
    }

    override fun onBindViewHolder(holder: PageItemViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    fun onNext(result: List<PageItem>) {
        val diffResult = DiffUtil.calculateDiff(PageItemDiffUtil(items, result))
        items.clear()
        items.addAll(result)
        diffResult.dispatchUpdatesTo(this)
    }

    private class PageItemDiffUtil(val items: List<PageItem>, val result: List<PageItem>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = result[newItemPosition]
            return oldItem.id == newItem.id && oldItem.type == newItem.type
        }

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = result.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return items[oldItemPosition] == result[newItemPosition]
        }
    }
}
