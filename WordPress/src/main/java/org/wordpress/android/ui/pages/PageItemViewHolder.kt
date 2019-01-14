package org.wordpress.android.ui.pages

import android.support.annotation.LayoutRes
import android.support.v4.widget.CompoundButtonCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.RadioButton
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.ParentPage
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DisplayUtils
import java.util.Date

sealed class PageItemViewHolder(internal val parent: ViewGroup, @LayoutRes layout: Int) :
        RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(pageItem: PageItem)

    class PageViewHolder(
        parentView: ViewGroup,
        private val onMenuAction: (PageItem.Action, Page) -> Boolean,
        private val onItemTapped: (Page) -> Unit
    ) : PageItemViewHolder(parentView, R.layout.page_list_item) {
        private val pageTitle = itemView.findViewById<TextView>(R.id.page_title)
        private val pageMore = itemView.findViewById<ImageButton>(R.id.page_more)
        private val time = itemView.findViewById<TextView>(R.id.time_posted)
        private val labels = itemView.findViewById<TextView>(R.id.labels)
        private val pageItemContainer = itemView.findViewById<ViewGroup>(R.id.page_item)

        override fun onBind(pageItem: PageItem) {
            (pageItem as Page).let {
                val indentWidth = DisplayUtils.dpToPx(parent.context, 16 * pageItem.indent)
                val marginLayoutParams = pageItemContainer.layoutParams as ViewGroup.MarginLayoutParams
                marginLayoutParams.leftMargin = indentWidth
                pageItemContainer.layoutParams = marginLayoutParams

                pageTitle.text = if (pageItem.title.isEmpty())
                    parent.context.getString(R.string.untitled_in_parentheses)
                else
                    pageItem.title

                val date = if (pageItem.date == Date(0)) Date() else pageItem.date
                time.text = DateTimeUtils.javaDateToTimeSpan(date, parent.context).capitalize()

                labels.text = pageItem.labels.map { parent.context.getString(it) }.sorted().joinToString(" · ")

                itemView.setOnClickListener { onItemTapped(pageItem) }

                pageMore.setOnClickListener { view -> moreClick(pageItem, view) }
                pageMore.visibility =
                        if (pageItem.actions.isNotEmpty() && pageItem.actionsEnabled) View.VISIBLE else View.INVISIBLE
            }
        }

        private fun moreClick(pageItem: PageItem.Page, v: View) {
            val popup = PopupMenu(v.context, v)
            popup.setOnMenuItemClickListener { item ->
                val action = PageItem.Action.fromItemId(item.itemId)
                onMenuAction(action, pageItem)
            }
            popup.menuInflater.inflate(R.menu.page_more, popup.menu)
            PageItem.Action.values().forEach {
                popup.menu.findItem(it.itemId).isVisible = pageItem.actions.contains(it)
            }
            popup.show()
        }
    }

    class PageDividerViewHolder(parentView: ViewGroup) : PageItemViewHolder(parentView, R.layout.page_divider_item) {
        private val dividerTitle = itemView.findViewById<TextView>(R.id.divider_text)

        override fun onBind(pageItem: PageItem) {
            (pageItem as Divider).apply {
                dividerTitle.text = pageItem.title
            }
        }
    }

    class PageParentViewHolder(
        parentView: ViewGroup,
        private val onParentSelected: (ParentPage) -> Unit,
        @LayoutRes layout: Int
    ) : PageItemViewHolder(parentView, layout) {
        private val pageTitle = itemView.findViewById<TextView>(R.id.page_title)
        private val radioButton = itemView.findViewById<RadioButton>(R.id.radio_button)

        override fun onBind(pageItem: PageItem) {
            (pageItem as ParentPage).apply {
                pageTitle.text = if (pageItem.title.isEmpty())
                    parent.context.getString(R.string.untitled_in_parentheses)
                else
                    pageItem.title
                radioButton.isChecked = pageItem.isSelected
                itemView.setOnClickListener {
                    onParentSelected(pageItem)
                }
                radioButton.setOnClickListener {
                    onParentSelected(pageItem)
                }

                @Suppress("DEPRECATION")
                CompoundButtonCompat.setButtonTintList(radioButton,
                        radioButton.resources.getColorStateList(R.color.dialog_compound_button_thumb))
            }
        }
    }

    class EmptyViewHolder(
        parentView: ViewGroup,
        private val onActionButtonClicked: () -> Unit
    ) : PageItemViewHolder(parentView, R.layout.page_empty_item) {
        private val emptyView = itemView.findViewById<ActionableEmptyView>(R.id.actionable_empty_view)

        @Suppress("DEPRECATION")
        override fun onBind(pageItem: PageItem) {
            if (pageItem is Empty) {
                emptyView.title.text = emptyView.resources.getString(pageItem.textResource)

                if (pageItem.isButtonVisible) {
                    emptyView.button.setOnClickListener {
                        onActionButtonClicked()
                    }
                    emptyView.button.visibility = View.VISIBLE
                } else {
                    emptyView.button.visibility = View.GONE
                }

                emptyView.image.visibility = if (pageItem.isImageVisible) View.VISIBLE else View.GONE

                emptyView.updateLayoutForSearch(pageItem.isSearching, 0)
            }
        }
    }
}
