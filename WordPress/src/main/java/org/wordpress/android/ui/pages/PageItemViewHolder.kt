package org.wordpress.android.ui.pages

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.core.view.MenuCompat
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.Page
import org.wordpress.android.ui.pages.PageItem.ParentPage
import org.wordpress.android.ui.pages.PageItem.VirtualHomepage.Action
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.QuickStartUtils
import org.wordpress.android.util.extensions.capitalizeWithLocaleWithoutLint
import org.wordpress.android.util.extensions.currentLocale
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.getDrawableFromAttribute
import org.wordpress.android.util.extensions.setVisible
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState
import java.util.Date
import java.util.Locale
import android.R as AndroidR
import androidx.appcompat.widget.PopupMenu as AppCompatPopupMenu
import com.google.android.material.R as MaterialR

sealed class PageItemViewHolder(internal val parent: ViewGroup, @LayoutRes layout: Int) :
    RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(pageItem: PageItem)

    class PageViewHolder(
        parentView: ViewGroup,
        private val onMenuAction: (PagesListAction, Page) -> Boolean,
        private val onItemTapped: (Page) -> Unit,
        private val imageManager: ImageManager? = null,
        private val isSitePhotonCapable: Boolean = false,
        private val isPrivateAtSite: Boolean = false,
        private val uiHelper: UiHelpers
    ) : PageItemViewHolder(parentView, R.layout.page_list_item) {
        private val pageTitle = itemView.findViewById<TextView>(R.id.page_title)
        private val pageMore = itemView.findViewById<ImageButton>(R.id.page_more)
        private val pageSubtitle = itemView.findViewById<TextView>(R.id.page_subtitle)
        private val pageSubtitleIcon = itemView.findViewById<ImageView>(R.id.page_subtitle_icon)
        private val pageSubtitleSuffix = itemView.findViewById<TextView>(R.id.page_subtitle_suffix)
        private val labels = itemView.findViewById<TextView>(R.id.labels)
        private val featuredImage = itemView.findViewById<ImageView>(R.id.featured_image)
        private val uploadProgressBar: ProgressBar = itemView.findViewById(R.id.upload_progress)
        private val disabledOverlay: FrameLayout = itemView.findViewById(R.id.disabled_overlay)
        private val pageItemContainer = itemView.findViewById<ViewGroup>(R.id.page_item)
        private val pageLayout = itemView.findViewById<ViewGroup>(R.id.page_layout)
        private val selectableBackground: Drawable? = parent.context.getDrawableFromAttribute(
            AndroidR.attr.selectableItemBackground
        )

        companion object {
            const val FEATURED_IMAGE_THUMBNAIL_SIZE_DP = 40
        }

        override fun onBind(pageItem: PageItem) {
            (pageItem as Page).let { page ->
                val indentWidth = DisplayUtils.dpToPx(parent.context, 16 * page.indent)
                val marginLayoutParams = pageItemContainer.layoutParams as ViewGroup.MarginLayoutParams
                marginLayoutParams.leftMargin = indentWidth
                pageItemContainer.layoutParams = marginLayoutParams

                pageTitle.text = page.title.ifEmpty {
                    parent.context.getString(R.string.untitled_in_parentheses)
                }

                showSubtitle(page.date, page.author, page.subtitle, page.icon)

                labels.text = page.labels.map { uiHelper.getTextOfUiString(parent.context, it).toString() }.sorted()
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

                itemView.setOnClickListener {
                    QuickStartUtils.removeQuickStartFocusPoint(pageItemContainer)
                    onItemTapped(page)
                }

                pageMore.setOnClickListener { view -> moreClick(page, view) }
                pageMore.visibility =
                    if (page.actions.isNotEmpty() && page.actionsEnabled) View.VISIBLE else View.INVISIBLE

                setBackground(page.tapActionEnabled)
                showFeaturedImage(page.imageUrl)

                uiHelper.updateVisibility(disabledOverlay, page.showOverlay)
                updateProgressBarState(page.progressBarUiState)

                // Clean up focus tip if there
                QuickStartUtils.removeQuickStartFocusPoint(pageItemContainer)
                if (page.showQuickStartFocusPoint) {
                    pageItemContainer.post {
                        val horizontalOffset = pageItemContainer.width / 2
                        val verticalOffset = (pageItemContainer.height)
                        QuickStartUtils.addQuickStartFocusPointAboveTheView(
                            pageItemContainer,
                            pageMore,
                            horizontalOffset,
                            -verticalOffset
                        )
                    }
                }
            }
        }

        private fun updateProgressBarState(progressBarUiState: ProgressBarUiState) {
            uiHelper.updateVisibility(uploadProgressBar, progressBarUiState.visibility)
            when (progressBarUiState) {
                is ProgressBarUiState.Indeterminate -> uploadProgressBar.isIndeterminate = true
                is ProgressBarUiState.Determinate -> {
                    uploadProgressBar.isIndeterminate = false
                    uploadProgressBar.progress = progressBarUiState.progress
                }

                is ProgressBarUiState.Hidden -> Unit // Do nothing
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
            val menu = AppCompatPopupMenu(v.context, v, GravityCompat.END)
            MenuCompat.setGroupDividerEnabled(menu.menu, true)
            menu.setForceShowIcon(true)

            pageItem.actions.forEach { singleItemAction ->
                val menuItem = menu.menu.add(
                    singleItemAction.actionGroup.id,
                    0,
                    Menu.NONE,
                    singleItemAction.title
                )
                menuItem.setOnMenuItemClickListener {
                    onMenuAction(singleItemAction, pageItem)
                    true
                }
                setIconAndIconColorIfNeeded(v.context, menuItem, singleItemAction)
                setTextColorIfNeeded(v.context, menuItem, singleItemAction)
            }
            menu.show()
        }

        private fun setIconAndIconColorIfNeeded(
            context: Context,
            menuItem: MenuItem,
            singleItemAction: PagesListAction,
        ) {
            val icon: Drawable = setTint(
                context,
                ContextCompat.getDrawable(context, singleItemAction.icon)!!,
                singleItemAction.colorTint
            )
            menuItem.icon = icon
        }

        private fun setTextColorIfNeeded(
            context: Context,
            menuItem: MenuItem,
            singleItemAction: PagesListAction
        ) {
            if (singleItemAction.colorTint > 0 &&
                singleItemAction.colorTint != MaterialR.attr.colorOnSurface) {
                val menuTitle = context.getText(singleItemAction.title)
                val spannableString = SpannableString(menuTitle)
                val textColor = context.getColorFromAttribute(singleItemAction.colorTint)

                spannableString.setSpan(
                    ForegroundColorSpan(textColor),
                    0,
                    spannableString.length,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )

                menuItem.title = spannableString
            }
        }


        private fun setTint(context: Context, drawable: Drawable, color: Int): Drawable {
            val wrappedDrawable: Drawable = DrawableCompat.wrap(drawable)
            if (color != 0) {
                val iconColor = context.getColorFromAttribute(color)
                DrawableCompat.setTint(wrappedDrawable, iconColor)
            }
            return wrappedDrawable
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

        private fun showSubtitle(inputDate: Date, author: String?, subtitle: Int?, icon: Int?) {
            val date = if (inputDate == Date(0)) Date() else inputDate
            val stringDate = DateTimeUtils.javaDateToTimeSpan(date, parent.context)
                .capitalizeWithLocaleWithoutLint(parent.context.currentLocale)

            /** The subtitle is split in two TextViews:
             * - Date and Author (if not null) occupy the [pageSubtitle] TextView
             * - Subtitle fills the [pageSubtitleSuffix] TextView
             */
            if (subtitle != null) {
                pageSubtitle.text = author?.let {
                    String.format(
                        Locale.getDefault(),
                        parent.context.getString(R.string.pages_item_date_author_subtitle),
                        stringDate,
                        it
                    )
                } ?: String.format(
                    Locale.getDefault(),
                    parent.context.getString(R.string.pages_item_date_subtitle),
                    stringDate
                )
                pageSubtitleSuffix.text = String.format(
                    Locale.getDefault(),
                    parent.context.getString(R.string.pages_item_subtitle),
                    parent.context.getString(subtitle)
                )
            } else {
                pageSubtitle.text = author?.let {
                    String.format(
                        Locale.getDefault(),
                        parent.context.getString(R.string.pages_item_date_author),
                        stringDate,
                        it
                    )
                } ?: stringDate
                @SuppressLint("SetTextI18n")
                pageSubtitleSuffix.text = ""
            }

            icon?.let { pageSubtitleIcon.setImageResource(it) }
            pageSubtitleIcon.setVisible(icon != null)
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
                pageTitle.text = if (pageItem.title.isEmpty()) {
                    parent.context.getString(R.string.untitled_in_parentheses)
                } else {
                    pageItem.title
                }
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

    class VirtualHomepageViewHolder(
        parentView: ViewGroup,
        private val onAction: (Action) -> Unit,
    ) : PageItemViewHolder(parentView, R.layout.page_virtual_homepage_item) {
        private val pageItemContainer = itemView.findViewById<ViewGroup>(R.id.page_item)
        private val pageItemInfo = itemView.findViewById<ImageButton>(R.id.page_info_icon)

        override fun onBind(pageItem: PageItem) {
            itemView.setOnClickListener {
                QuickStartUtils.removeQuickStartFocusPoint(pageItemContainer)
                onAction(Action.OpenSiteEditor)
            }
            pageItemInfo.setOnClickListener {
                onAction(Action.OpenExternalLink.TemplateSupport)
            }
        }
    }
}
