package org.wordpress.android.ui.pages

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.layout
import org.wordpress.android.ui.pages.PagesAdapter.PageItemViewHolder
import org.wordpress.android.ui.pages.PagesAdapter.PageItemViewHolder.PageDividerViewHolder
import org.wordpress.android.ui.pages.PagesAdapter.PageItemViewHolder.PageViewHolder

class PagesAdapter : RecyclerView.Adapter<PageItemViewHolder>() {
    private val items = mutableListOf<PageItem>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageItemViewHolder {
        val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            PageItem.PAGE -> PageViewHolder(layoutInflater, parent) { showMenu(it) }
            PageItem.DIVIDER -> PageDividerViewHolder(layoutInflater, parent)
            else -> throw IllegalArgumentException("Unexpected view type")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return items[position].viewType
    }

    override fun onBindViewHolder(holder: PageItemViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    fun onNext(result: List<PageItem>) {
        val diffResult = DiffUtil.calculateDiff(PageDiffUtil(items, result))
        items.clear()
        items.addAll(result)
        diffResult.dispatchUpdatesTo(this)
    }

    fun showMenu(v: View) {
        val popup = PopupMenu(v.context, v)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.view_page -> TODO()
                R.id.set_parent -> TODO()
                R.id.publish_now -> TODO()
                R.id.move_to_draft -> TODO()
                R.id.move_to_trash -> TODO()
                else -> {
                    throw IllegalArgumentException("Unexpected item in contextual menu")
                }
            }
        }
        popup.menuInflater.inflate(R.menu.page_more, popup.menu)
        popup.show()
    }

    sealed class PageItemViewHolder(parentView: View) :
            ViewHolder(parentView) {
        abstract fun onBind(pageItem: PageItem)

        class PageViewHolder(
            layoutInflater: LayoutInflater,
            parentView: ViewGroup,
            private val moreClick: (View) -> Unit) :
                PageItemViewHolder(layoutInflater.inflate(layout.page_list_item, parentView, false)) {
            private val firstIndent = itemView.findViewById<View>(R.id.first_indent)
            private val secondIndent = itemView.findViewById<View>(R.id.second_indent)
            private val thirdIndent = itemView.findViewById<View>(R.id.third_indent)
            private val fourthIndent = itemView.findViewById<View>(R.id.fourth_indent)
            private val fifthIndent = itemView.findViewById<View>(R.id.fifth_indent)
            private val pageTitle = itemView.findViewById<TextView>(R.id.page_title)
            private val pageMore = itemView.findViewById<Button>(R.id.page_more)

            override fun onBind(pageItem: PageItem) {
                (pageItem as PageItem.Page).apply {
                    firstIndent.visibility = if (pageItem.indent > 0) View.VISIBLE else View.GONE
                    secondIndent.visibility = if (pageItem.indent > 1) View.VISIBLE else View.GONE
                    thirdIndent.visibility = if (pageItem.indent > 2) View.VISIBLE else View.GONE
                    fourthIndent.visibility = if (pageItem.indent > 3) View.VISIBLE else View.GONE
                    fifthIndent.visibility = if (pageItem.indent > 4) View.VISIBLE else View.GONE
                    pageTitle.text = pageItem.title
                    pageMore.setOnClickListener { moreClick(it) }
                }
            }
        }

        class PageDividerViewHolder(layoutInflater: LayoutInflater, parentView: ViewGroup) :
                PageItemViewHolder(layoutInflater.inflate(layout.page_divider_item, parentView, false)) {
            private val dividerTitle = itemView.findViewById<TextView>(R.id.divider_text)
            override fun onBind(pageItem: PageItem) {
                (pageItem as PageItem.Divider).apply {
                    dividerTitle.text = pageItem.title
                }
            }
        }
    }

    private class PageDiffUtil(val items: List<PageItem>, val result: List<PageItem>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = result[newItemPosition]
            return oldItem.id == newItem.id && oldItem.viewType == newItem.viewType
        }

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = result.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return items[oldItemPosition] == result[newItemPosition]
        }
    }
}
