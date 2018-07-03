package org.wordpress.android.ui.pages

import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.util.DisplayUtils

sealed class PageItemViewHolder(parentView: View) :
        ViewHolder(parentView) {
    abstract fun onBind(pageItem: PageItem)

    class PageViewHolder(
        layoutInflater: LayoutInflater,
        parentView: ViewGroup,
        private val onAction: (PageItem.Action, PageItem) -> Boolean
    ) : PageItemViewHolder(layoutInflater.inflate(layout.page_list_item, parentView, false)) {
        private val indentContainer = itemView.findViewById<FrameLayout>(id.indent_container)
        private val pageTitle = itemView.findViewById<TextView>(id.page_title)
        private val pageMore = itemView.findViewById<ImageButton>(id.page_more)

        override fun onBind(pageItem: PageItem) {
            (pageItem as Page).apply {
                if (pageItem.indent > 0) {
                    val sumIndent = 16 * pageItem.indent
                    val indentWidth = DisplayUtils.dpToPx(indentContainer.context, sumIndent)
                    val layoutParams = ViewGroup.LayoutParams(indentWidth, ViewGroup.LayoutParams.MATCH_PARENT)
                    val indent = View(indentContainer.context)
                    indentContainer.addView(indent, layoutParams)
                }
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

    class EmptyViewHolder(layoutInflater: LayoutInflater, parentView: ViewGroup) :
            PageItemViewHolder(layoutInflater.inflate(layout.page_empty_item, parentView, false)) {
        private val emptyView = itemView.findViewById<TextView>(id.empty_view)
        override fun onBind(pageItem: PageItem) {
            (pageItem as Empty).apply {
                pageItem.textResource?.let {
                    emptyView.text = emptyView.resources.getText(it)
                }
            }
        }
    }
}
