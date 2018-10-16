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
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.ParentPage
import org.wordpress.android.util.DisplayUtils

sealed class PageItemViewHolder(internal val parent: ViewGroup, @LayoutRes layout: Int) :
        RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(pageItem: PageItem)

    class PageViewHolder(
        parentView: ViewGroup,
        private val onMenuAction: (PageItem.Action, Page) -> Boolean,
        private val onItemTapped: (Page) -> Unit
    ) : PageItemViewHolder(parentView, layout.page_list_item) {
        private val pageTitle = itemView.findViewById<TextView>(id.page_title)
        private val pageMore = itemView.findViewById<ImageButton>(id.page_more)
        private val firstLabel = itemView.findViewById<TextView>(id.first_label)
        private val secondLabel = itemView.findViewById<TextView>(id.second_label)
        private val pageItemContainer = itemView.findViewById<ViewGroup>(id.page_item)
        private val largeStretcher = itemView.findViewById<View>(id.large_stretcher)
        private val smallStretcher = itemView.findViewById<View>(id.small_stretcher)

        override fun onBind(pageItem: PageItem) {
            (pageItem as Page).apply {
                val indentWidth = DisplayUtils.dpToPx(parent.context, 16 * pageItem.indent)
                val marginLayoutParams = pageItemContainer.layoutParams as ViewGroup.MarginLayoutParams
                marginLayoutParams.leftMargin = indentWidth
                pageItemContainer.layoutParams = marginLayoutParams

                pageTitle.text = if (pageItem.title.isEmpty())
                    parent.context.getString(R.string.untitled_in_parentheses)
                else
                    pageItem.title

                if (pageItem.labels.isEmpty()) {
                    firstLabel.visibility = View.GONE
                    smallStretcher.visibility = View.VISIBLE
                    largeStretcher.visibility = View.GONE
                } else {
                    firstLabel.text = parent.context.getString(pageItem.labels.first())
                    firstLabel.visibility = View.VISIBLE
                    smallStretcher.visibility = View.GONE
                    largeStretcher.visibility = View.VISIBLE
                }

                if (pageItem.labels.size <= 1) {
                    secondLabel.visibility = View.GONE
                } else {
                    secondLabel.text = parent.context.getString(pageItem.labels[1])
                    secondLabel.visibility = View.VISIBLE
                }

                itemView.setOnClickListener { onItemTapped(pageItem) }

                pageMore.setOnClickListener { moreClick(pageItem, it) }
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

    class PageDividerViewHolder(parentView: ViewGroup) : PageItemViewHolder(parentView, layout.page_divider_item) {
        private val dividerTitle = itemView.findViewById<TextView>(id.divider_text)

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
        private val pageTitle = itemView.findViewById<TextView>(id.page_title)
        private val radioButton = itemView.findViewById<RadioButton>(id.radio_button)

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
    ) : PageItemViewHolder(parentView, layout.page_empty_item) {
        private val emptyView = itemView.findViewById<ActionableEmptyView>(id.actionable_empty_view)

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
