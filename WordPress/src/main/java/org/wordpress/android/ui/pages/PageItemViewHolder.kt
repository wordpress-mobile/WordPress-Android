package org.wordpress.android.ui.pages

import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.Page

sealed class PageItemViewHolder(parentView: View) :
        ViewHolder(parentView) {
    abstract fun onBind(pageItem: PageItem)

    class PageViewHolder(
        layoutInflater: LayoutInflater,
        parentView: ViewGroup,
        private val onAction: (PageItem.Action, PageItem) -> Boolean) :
            PageItemViewHolder(layoutInflater.inflate(layout.page_list_item, parentView, false)) {
        private val firstIndent = itemView.findViewById<View>(id.first_indent)
        private val secondIndent = itemView.findViewById<View>(id.second_indent)
        private val thirdIndent = itemView.findViewById<View>(id.third_indent)
        private val fourthIndent = itemView.findViewById<View>(id.fourth_indent)
        private val fifthIndent = itemView.findViewById<View>(id.fifth_indent)
        private val sixthIndent = itemView.findViewById<View>(id.sixth_indent)
        private val seventhIndent = itemView.findViewById<View>(id.seventh_indent)
        private val eightIndent = itemView.findViewById<View>(id.eight_indent)
        private val pageTitle = itemView.findViewById<TextView>(id.page_title)
        private val pageMore = itemView.findViewById<ImageButton>(id.page_more)

        override fun onBind(pageItem: PageItem) {
            (pageItem as Page).apply {
                firstIndent.visibility = if (pageItem.indent > 0) View.VISIBLE else View.GONE
                secondIndent.visibility = if (pageItem.indent > 1) View.VISIBLE else View.GONE
                thirdIndent.visibility = if (pageItem.indent > 2) View.VISIBLE else View.GONE
                fourthIndent.visibility = if (pageItem.indent > 3) View.VISIBLE else View.GONE
                fifthIndent.visibility = if (pageItem.indent > 4) View.VISIBLE else View.GONE
                sixthIndent.visibility = if (pageItem.indent > 5) View.VISIBLE else View.GONE
                seventhIndent.visibility = if (pageItem.indent > 6) View.VISIBLE else View.GONE
                eightIndent.visibility = if (pageItem.indent > 7) View.VISIBLE else View.GONE
                pageTitle.text = pageItem.title
                pageMore.setOnClickListener { moreClick(pageItem, it) }
                pageMore.visibility = if (pageItem.enabledActions.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }

        private fun moreClick(pageItem: PageItem.Page, v: View) {
            val popup = PopupMenu(v.context, v)
            popup.setOnMenuItemClickListener { item ->
                val action = PageItem.Action.fromItemId(item.itemId)
                onAction(action, pageItem)
            }
            popup.menuInflater.inflate(R.menu.page_more, popup.menu)
            PageItem.Action.values().forEach {
                popup.menu.findItem(it.itemId).isVisible = pageItem.enabledActions.contains(it)
            }
            popup.show()
        }
    }

    class PageDividerViewHolder(layoutInflater: LayoutInflater, parentView: ViewGroup) :
            PageItemViewHolder(layoutInflater.inflate(layout.page_divider_item, parentView, false)) {
        private val dividerTitle = itemView.findViewById<TextView>(id.divider_text)
        override fun onBind(pageItem: PageItem) {
            (pageItem as Divider).apply {
                dividerTitle.text = pageItem.title
            }
        }
    }
}
