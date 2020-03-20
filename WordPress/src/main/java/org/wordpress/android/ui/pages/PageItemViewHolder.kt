package org.wordpress.android.ui.pages

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.PopupMenu
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.widget.CompoundButtonCompat
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.ParentPage
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.util.currentLocale
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.capitalizeWithLocaleWithoutLint
import org.wordpress.android.util.getDrawableFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import java.util.Date

sealed class PageItemViewHolder(internal val parent: ViewGroup, @LayoutRes layout: Int) :
        RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(pageItem: PageItem)

    class PageViewHolder(
        parentView: ViewGroup,
        private val onMenuAction: (PageItem.Action, Page) -> Boolean,
        private val onItemTapped: (Page) -> Unit,
        private val imageManager: ImageManager? = null,
        private val isSitePhotonCapable: Boolean = false
    ) : PageItemViewHolder(parentView, R.layout.page_list_item) {
        private val pageTitle = itemView.findViewById<TextView>(R.id.page_title)
        private val pageMore = itemView.findViewById<ImageButton>(R.id.page_more)
        private val time = itemView.findViewById<TextView>(R.id.time_posted)
        private val labels = itemView.findViewById<TextView>(R.id.labels)
        private val featuredImage = itemView.findViewById<ImageView>(R.id.featured_image)
        private val pageItemContainer = itemView.findViewById<ViewGroup>(R.id.page_item)
        private val pageLayout = itemView.findViewById<ViewGroup>(R.id.page_layout)
        private val selectableBackground: Drawable? = parent.context.getDrawableFromAttribute(
                android.R.attr.selectableItemBackground
        )

        companion object {
            const val FEATURED_IMAGE_THUMBNAIL_SIZE_DP = 40
        }

        @ExperimentalStdlibApi
        override fun onBind(pageItem: PageItem) {
            (pageItem as Page).let { page ->
                val indentWidth = DisplayUtils.dpToPx(parent.context, 16 * page.indent)
                val marginLayoutParams = pageItemContainer.layoutParams as ViewGroup.MarginLayoutParams
                marginLayoutParams.leftMargin = indentWidth
                pageItemContainer.layoutParams = marginLayoutParams

                pageTitle.text = if (page.title.isEmpty())
                    parent.context.getString(R.string.untitled_in_parentheses)
                else
                    page.title

                val date = if (page.date == Date(0)) Date() else page.date
                time.text = DateTimeUtils.javaDateToTimeSpan(date, parent.context)
                        .capitalizeWithLocaleWithoutLint(parent.context.currentLocale)

                if (page.labels.isNotEmpty()) {
                    labels.text = page.labels.map { parent.context.getString(it) }.sorted()
                            .joinToString(prefix = " · ", separator = " · ")
                }

                itemView.setOnClickListener { onItemTapped(page) }

                pageMore.setOnClickListener { view -> moreClick(page, view) }
                pageMore.visibility =
                        if (page.actions.isNotEmpty() && page.actionsEnabled) View.VISIBLE else View.INVISIBLE

                setBackground(page.tapActionEnabled)
                showFeaturedImage(page.imageUrl)
            }
        }

        private fun setBackground(tapActionEnabled: Boolean) {
            if (tapActionEnabled) {
                pageLayout.background = selectableBackground
            } else {
                pageLayout.background = null
            }
        }

        private fun moreClick(pageItem: Page, v: View) {
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

        private fun showFeaturedImage(imageUrl: String?) {
            val imageSize = DisplayUtils.dpToPx(parent.context, FEATURED_IMAGE_THUMBNAIL_SIZE_DP)
            if (imageUrl == null) {
                featuredImage.visibility = View.GONE
                imageManager?.cancelRequestAndClearImageView(featuredImage)
            } else if (imageUrl.startsWith("http")) {
                featuredImage.visibility = View.VISIBLE
                val photonUrl = ReaderUtils.getResizedImageUrl(imageUrl, imageSize, imageSize, !isSitePhotonCapable)
                imageManager?.load(featuredImage, ImageType.PHOTO, photonUrl, ScaleType.CENTER_CROP)
            } else {
                val bmp = ImageUtils.getWPImageSpanThumbnailFromFilePath(featuredImage.context, imageUrl, imageSize)
                if (bmp != null) {
                    featuredImage.visibility = View.VISIBLE
                    imageManager?.load(featuredImage, bmp)
                } else {
                    featuredImage.visibility = View.GONE
                    imageManager?.cancelRequestAndClearImageView(featuredImage)
                }
            }
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
                        radioButton.resources.getColorStateList(R.color.primary_40_gray_20_gray_40_selector))
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
            (pageItem as Empty).apply {
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
