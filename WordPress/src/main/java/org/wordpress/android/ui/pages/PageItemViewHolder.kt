package org.wordpress.android.ui.pages

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.ParentPage
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.capitalizeWithLocaleWithoutLint
import org.wordpress.android.util.currentLocale
import org.wordpress.android.util.getDrawableFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState.Determinate
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState.Indeterminate
import java.util.Date
import java.util.Locale

sealed class PageItemViewHolder(internal val parent: ViewGroup, @LayoutRes layout: Int) :
        RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(pageItem: PageItem)

    class PageViewHolder(
        parentView: ViewGroup,
        private val onMenuAction: (PageItem.Action, Page) -> Boolean,
        private val onItemTapped: (Page) -> Unit,
        private val imageManager: ImageManager? = null,
        private val isSitePhotonCapable: Boolean = false,
        private val isPrivateAtSite: Boolean = false,
        private val uiHelper: UiHelpers
    ) : PageItemViewHolder(parentView, R.layout.page_list_item) {
        private val pageTitle = itemView.findViewById<TextView>(R.id.page_title)
        private val pageMore = itemView.findViewById<ImageButton>(R.id.page_more)
        private val pageSubtitle = itemView.findViewById<TextView>(R.id.page_subtitle)
        private val labels = itemView.findViewById<TextView>(R.id.labels)
        private val featuredImage = itemView.findViewById<ImageView>(R.id.featured_image)
        private val uploadProgressBar: ProgressBar = itemView.findViewById(R.id.upload_progress)
        private val disabledOverlay: FrameLayout = itemView.findViewById(R.id.disabled_overlay)
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

                showSubtitle(page.date, page.author, page.subtitle)

                labels.text = page.labels.map { uiHelper.getTextOfUiString(parent.context, it) }.sorted()
                        .joinToString(separator = " · ")
                page.labelsColor?.let { labelsColor ->
                    labels.setTextColor(
                            ContextCompat.getColor(
                                    itemView.context,
                                    labelsColor
                            )
                    )
                }

                uiHelper.updateVisibility(labels, page.labels.isNotEmpty())

                itemView.setOnClickListener { onItemTapped(page) }

                pageMore.setOnClickListener { view -> moreClick(page, view) }
                pageMore.visibility =
                        if (page.actions.isNotEmpty() && page.actionsEnabled) View.VISIBLE else View.INVISIBLE

                setBackground(page.tapActionEnabled)
                showFeaturedImage(page.imageUrl)

                uiHelper.updateVisibility(disabledOverlay, page.showOverlay)
                updateProgressBarState(page.progressBarUiState)
            }
        }

        private fun updateProgressBarState(progressBarUiState: ProgressBarUiState) {
            uiHelper.updateVisibility(uploadProgressBar, progressBarUiState.visibility)
            when (progressBarUiState) {
                Indeterminate -> uploadProgressBar.isIndeterminate = true
                is Determinate -> {
                    uploadProgressBar.isIndeterminate = false
                    uploadProgressBar.progress = progressBarUiState.progress
                }
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
                val photonUrl = ReaderUtils.getResizedImageUrl(
                        imageUrl, imageSize, imageSize, !isSitePhotonCapable, isPrivateAtSite
                )
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

        @ExperimentalStdlibApi
        private fun showSubtitle(inputDate: Date, author: String?, subtitle: Int?) {
            val date = if (inputDate == Date(0)) Date() else inputDate
            val stringDate = DateTimeUtils.javaDateToTimeSpan(date, parent.context)
                    .capitalizeWithLocaleWithoutLint(parent.context.currentLocale)

            /** The subtitle can use 2 or 3 placeholders
            * Date - Only (author & subtitle are null)
            * Date - Author (author != null && subtitle == null)
            * Date - subtitle (author == null && subtitle != null)
            * Date - Author - subtitle (all have values)
            */
            pageSubtitle.text = if (author == null && subtitle == null) {
                stringDate
            } else if (author != null && subtitle == null) {
                String.format(
                        Locale.getDefault(),
                        parent.context.getString(R.string.pages_item_subtitle),
                        stringDate,
                        author)
            } else if (author == null && subtitle != null) {
                String.format(
                        Locale.getDefault(),
                        parent.context.getString(R.string.pages_item_subtitle),
                        stringDate,
                        parent.context.getString(subtitle))
            } else {
                String.format(
                        Locale.getDefault(),
                        parent.context.getString(R.string.pages_item_subtitle_date_author),
                        stringDate,
                        author,
                        parent.context.getString(subtitle!!))
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
