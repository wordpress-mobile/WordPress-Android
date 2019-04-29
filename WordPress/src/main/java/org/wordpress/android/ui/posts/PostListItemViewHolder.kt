package org.wordpress.android.ui.posts

import android.support.annotation.LayoutRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.PopupMenu
import android.widget.ProgressBar
import org.wordpress.android.R
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.posts.PostListItemAction
import org.wordpress.android.viewmodel.posts.PostListItemAction.MoreItem
import org.wordpress.android.viewmodel.posts.PostListItemAction.SingleItem
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import org.wordpress.android.widgets.PostListButton
import org.wordpress.android.widgets.WPTextView

class PostListItemViewHolder(
    @LayoutRes layout: Int,
    parentView: ViewGroup,
    private val config: PostViewHolderConfig,
    private val uiHelpers: UiHelpers
) : RecyclerView.ViewHolder(LayoutInflater.from(parentView.context).inflate(layout, parentView, false)) {
    private val featuredImageView: ImageView = itemView.findViewById(R.id.image_featured)
    private val titleTextView: WPTextView = itemView.findViewById(R.id.title)
    private val excerptTextView: WPTextView = itemView.findViewById(R.id.excerpt)
    private val dateAndAuthorTextView: WPTextView = itemView.findViewById(R.id.date_and_author)
    private val statusesLabelTextView: WPTextView = itemView.findViewById(R.id.statuses_label)
    private val uploadProgressBar: ProgressBar = itemView.findViewById(R.id.upload_progress)
    private val disabledOverlay: FrameLayout = itemView.findViewById(R.id.disabled_overlay)
    private val actionButtons: List<PostListButton> = listOf(
            itemView.findViewById(R.id.btn_primary),
            itemView.findViewById(R.id.btn_secondary),
            itemView.findViewById(R.id.btn_ternary)
    )

    fun onBind(item: PostListItemUiState) {
        showFeaturedImage(item.data.imageUrl)
        uiHelpers.setTextOrHide(titleTextView, item.data.title)
        uiHelpers.setTextOrHide(excerptTextView, item.data.excerpt)
        uiHelpers.setTextOrHide(dateAndAuthorTextView, item.data.dateAndAuthor)
        uiHelpers.updateVisibility(statusesLabelTextView, item.data.statuses.isNotEmpty())
        updateStatusesLabel(statusesLabelTextView, item.data.statuses, item.data.statusesDelimiter)
        if (item.data.statusesColor != null) {
            statusesLabelTextView.setTextColor(
                    ContextCompat.getColor(
                            statusesLabelTextView.context,
                            item.data.statusesColor
                    )
            )
        }
        uiHelpers.updateVisibility(uploadProgressBar, item.data.showProgress)
        uiHelpers.updateVisibility(disabledOverlay, item.data.showOverlay)
        itemView.setOnClickListener { item.onSelected.invoke() }

        actionButtons.forEachIndexed { index, button ->
            updateMenuItem(button, item.actions.getOrNull(index))
        }
    }

    private fun updateStatusesLabel(view: WPTextView, statuses: List<UiString>, delimiter: UiString) {
        val separator = uiHelpers.getTextOfUiString(view.context, delimiter)
        view.text = statuses.joinToString(separator) { uiHelpers.getTextOfUiString(view.context, it) }
    }

    private fun updateMenuItem(postListButton: PostListButton, action: PostListItemAction?) {
        uiHelpers.updateVisibility(postListButton, action != null)
        if (action != null) {
            when (action) {
                is SingleItem -> {
                    postListButton.updateButtonType(action.buttonType)
                    postListButton.setOnClickListener { action.onButtonClicked.invoke(action.buttonType) }
                }
                is MoreItem -> {
                    postListButton.updateButtonType(action.buttonType)
                    postListButton.setOnClickListener { view ->
                        action.onButtonClicked.invoke(action.buttonType)
                        onMoreClicked(action, view)
                    }
                }
            }
        }
    }

    private fun onMoreClicked(moreActionItem: MoreItem, v: View) {
        val menu = PopupMenu(v.context, v)
        moreActionItem.actions.forEach { singleItemAction ->
            val menuItem = menu.menu.add(
                    Menu.NONE,
                    singleItemAction.buttonType.value,
                    Menu.NONE,
                    singleItemAction.buttonType.textResId
            )
            menuItem.setOnMenuItemClickListener {
                singleItemAction.onButtonClicked.invoke(singleItemAction.buttonType)
                true
            }
        }
        menu.show()
    }

    private fun showFeaturedImage(imageUrl: String?) {
        if (imageUrl == null) {
            featuredImageView.visibility = View.GONE
            config.imageManager.cancelRequestAndClearImageView(featuredImageView)
        } else if (imageUrl.startsWith("http")) {
            val photonUrl = ReaderUtils.getResizedImageUrl(
                    imageUrl, config.photonWidth, config.photonHeight, !config.isPhotonCapable
            )
            featuredImageView.visibility = View.VISIBLE
            config.imageManager.load(featuredImageView, ImageType.PHOTO, photonUrl, ScaleType.CENTER_CROP)
        } else {
            val bmp = ImageUtils.getWPImageSpanThumbnailFromFilePath(
                    featuredImageView.context, imageUrl, config.photonWidth
            )
            if (bmp != null) {
                featuredImageView.visibility = View.VISIBLE
                config.imageManager.load(featuredImageView, bmp)
            } else {
                featuredImageView.visibility = View.GONE
                config.imageManager.cancelRequestAndClearImageView(featuredImageView)
            }
        }
    }
}
