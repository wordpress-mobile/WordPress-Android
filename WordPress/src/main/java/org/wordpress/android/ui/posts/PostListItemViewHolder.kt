package org.wordpress.android.ui.posts

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
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
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.posts.PostListItemAction.MoreItem
import org.wordpress.android.viewmodel.posts.PostListItemAction.SingleItem
import org.wordpress.android.viewmodel.posts.PostListItemUiState
import org.wordpress.android.widgets.PostListButton
import org.wordpress.android.widgets.WPTextView

class PostListItemViewHolder(
    @LayoutRes layout: Int,
    parentView: ViewGroup,
    private val config: PostViewHolderConfig,
    private val uiHelpers: UiHelpers
) : RecyclerView.ViewHolder(
        LayoutInflater.from(parentView.context).inflate(
                layout,
                parentView,
                false
        )
) {
    private val ivImageFeatured: ImageView = itemView.findViewById(R.id.image_featured)
    private val tvTitle: WPTextView = itemView.findViewById(R.id.title)
    private val tvExcerpt: WPTextView = itemView.findViewById(R.id.excerpt)
    private val tvDateAndAuthor: WPTextView = itemView.findViewById(R.id.date_and_author)
    private val tvStatusLabels: WPTextView = itemView.findViewById(R.id.status_labels)
    private val pbProgress: ProgressBar = itemView.findViewById(R.id.progress)
    private val flDisabledOverlay: FrameLayout = itemView.findViewById(R.id.disabled_overlay)
    private val btnsList: List<PostListButton> = listOf(
            itemView.findViewById(R.id.btn_primary),
            itemView.findViewById(R.id.btn_secondary),
            itemView.findViewById(R.id.btn_ternary)
    )

    fun onBind(item: PostListItemUiState) {
        showFeaturedImage(item.imageUrl)
        setTextOrHide(tvTitle, item.title)
        setTextOrHide(tvExcerpt, item.excerpt)
        setTextOrHide(tvDateAndAuthor, item.dateAndAuthor)
        setTextOrHide(tvStatusLabels, item.statusLabels)
        if (item.statusLabelsColor != null) {
            tvStatusLabels.setTextColor(tvStatusLabels.context.resources.getColor(item.statusLabelsColor))
        }
        uiHelpers.updateVisibility(pbProgress, item.showProgress)
        uiHelpers.updateVisibility(flDisabledOverlay, item.showOverlay)
        itemView.setOnClickListener { item.onSelected.invoke() }

        if (btnsList.size < item.actions.size) {
            AppLog.e(AppLog.T.POSTS, "Some actions will not be displayed - max number of actions is ${btnsList.size}")
        }

        btnsList.forEachIndexed { index, button ->
            val actionAvailable = item.actions.size > index
            uiHelpers.updateVisibility(button, actionAvailable)
            if (actionAvailable) {
                val actionItem = item.actions[index]
                when (actionItem) {
                    is SingleItem -> {
                        button.buttonType = actionItem.buttonType
                        button.setOnClickListener { actionItem.onButtonClicked.invoke(actionItem.buttonType) }
                    }
                    is MoreItem -> {
                        button.buttonType = actionItem.buttonType
                        button.setOnClickListener { view ->
                            actionItem.onButtonClicked.invoke(actionItem.buttonType)
                            moreClick(actionItem, view)
                        }
                    }
                }
            }
        }
    }

    private fun moreClick(moreActionItem: MoreItem, v: View) {
        val popup = PopupMenu(v.context, v)
        popup.menuInflater.inflate(R.menu.posts_more, popup.menu)
        moreActionItem.actions.forEach { singleItemAction ->
            val menuItem = popup.menu.add(
                    0,
                    singleItemAction.buttonType.value,
                    0,
                    PostListButton.getButtonTextResId(singleItemAction.buttonType)
            )
            menuItem.setOnMenuItemClickListener {
                singleItemAction.onButtonClicked.invoke(singleItemAction.buttonType)
                true
            }
        }
        popup.show()
    }

    private fun showFeaturedImage(imageUrl: String?) {
        // TODO move part of this logic to VM
        if (imageUrl == null) {
            ivImageFeatured.visibility = View.GONE
            config.imageManager.cancelRequestAndClearImageView(ivImageFeatured)
        } else if (imageUrl.startsWith("http")) {
            val photonUrl = ReaderUtils.getResizedImageUrl(
                    imageUrl, config.photonWidth, config.photonHeight, !config.isPhotonCapable
            )
            ivImageFeatured.visibility = View.VISIBLE
            config.imageManager.load(ivImageFeatured, ImageType.PHOTO, photonUrl, ScaleType.CENTER_CROP)
        } else {
            val bmp = ImageUtils.getWPImageSpanThumbnailFromFilePath(
                    ivImageFeatured.context, imageUrl, config.photonWidth
            )
            if (bmp != null) {
                ivImageFeatured.visibility = View.VISIBLE
                config.imageManager.load(ivImageFeatured, bmp)
            } else {
                ivImageFeatured.visibility = View.GONE
                config.imageManager.cancelRequestAndClearImageView(ivImageFeatured)
            }
        }
    }

    private fun setTextOrHide(view: WPTextView, text: UiString?) {
        uiHelpers.updateVisibility(view, text != null)
        text?.let {
            view.text = uiHelpers.getTextOfUiString(itemView.context, it)
        }
    }
}
