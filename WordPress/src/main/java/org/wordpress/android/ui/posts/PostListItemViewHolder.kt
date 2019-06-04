package org.wordpress.android.ui.posts

import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.PopupMenu
import android.widget.ProgressBar
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import org.wordpress.android.R
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.posts.PostListItemAction
import org.wordpress.android.viewmodel.posts.PostListItemAction.MoreItem
import org.wordpress.android.viewmodel.posts.PostListItemAction.SingleItem
import org.wordpress.android.viewmodel.posts.PostListItemProgressBar
import org.wordpress.android.viewmodel.posts.PostListItemProgressBar.Determinate
import org.wordpress.android.viewmodel.posts.PostListItemProgressBar.Indeterminate
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import org.wordpress.android.viewmodel.posts.PostListItemUiStateData
import org.wordpress.android.widgets.PostListButton
import org.wordpress.android.widgets.WPTextView

sealed class PostListItemViewHolder(
    @LayoutRes layout: Int,
    parent: ViewGroup,
    private val config: PostViewHolderConfig,
    private val uiHelpers: UiHelpers
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    private val featuredImageView: ImageView = itemView.findViewById(R.id.image_featured)
    private val titleTextView: WPTextView = itemView.findViewById(R.id.title)
    private val dateTextView: WPTextView = itemView.findViewById(R.id.date)
    private val statusesTextView: WPTextView = itemView.findViewById(R.id.statuses_label)
    private val uploadProgressBar: ProgressBar = itemView.findViewById(R.id.upload_progress)
    private val disabledOverlay: FrameLayout = itemView.findViewById(R.id.disabled_overlay)

    /**
     * Url of an image loaded in the `featuredImageView`.
     */
    private var loadedFeaturedImgUrl: String? = null

    abstract fun onBind(item: PostListItemUiState)

    class Standard(
        parent: ViewGroup,
        config: PostViewHolderConfig,
        private val uiHelpers: UiHelpers
    ) : PostListItemViewHolder(R.layout.post_list_item, parent, config, uiHelpers) {
        private val excerptTextView: WPTextView = itemView.findViewById(R.id.excerpt)
        private val actionButtons: List<PostListButton> = listOf(
                itemView.findViewById(R.id.btn_primary),
                itemView.findViewById(R.id.btn_secondary),
                itemView.findViewById(R.id.btn_ternary)
        )

        override fun onBind(item: PostListItemUiState) {
            setBasicValues(item.data)

            uiHelpers.setTextOrHide(excerptTextView, item.data.excerpt)
            itemView.setOnClickListener { item.onSelected.invoke() }

            actionButtons.forEachIndexed { index, button ->
                updateMenuItem(button, item.actions.getOrNull(index))
            }
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
                            onMoreClicked(action.actions, view)
                        }
                    }
                }
            }
        }
    }

    class Compact(
        parent: ViewGroup,
        config: PostViewHolderConfig,
        private val uiHelpers: UiHelpers
    ) : PostListItemViewHolder(R.layout.post_list_item_compact, parent, config, uiHelpers) {
        private val moreButton: ImageButton = itemView.findViewById(R.id.more_button)

        override fun onBind(item: PostListItemUiState) {
            setBasicValues(item.data)

            itemView.setOnClickListener { item.onSelected.invoke() }
            uiHelpers.updateVisibility(moreButton, item.compactActions.actions.isNotEmpty())
            moreButton.setOnClickListener { onMoreClicked(item.compactActions.actions, moreButton) }
        }
    }

    protected fun setBasicValues(data: PostListItemUiStateData) {
        uiHelpers.setTextOrHide(titleTextView, data.title)
        uiHelpers.setTextOrHide(dateTextView, data.date)
        uiHelpers.updateVisibility(statusesTextView, data.statuses.isNotEmpty())
        updateStatusesLabel(statusesTextView, data.statuses, data.statusesDelimiter, data.statusesColor)
        showFeaturedImage(data.imageUrl)
        updateProgressBarState(data.progressBarState)
        uiHelpers.updateVisibility(disabledOverlay, data.showOverlay)
    }

    protected fun onMoreClicked(actions: List<PostListItemAction>, v: View) {
        val menu = PopupMenu(v.context, v)
        actions.forEach { singleItemAction ->
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

    private fun updateProgressBarState(progressBarState: PostListItemProgressBar) {
        uiHelpers.updateVisibility(uploadProgressBar, progressBarState.visibility)
        when (progressBarState) {
            Indeterminate -> uploadProgressBar.isIndeterminate = true
            is Determinate -> {
                uploadProgressBar.isIndeterminate = false
                uploadProgressBar.progress = progressBarState.progress
            }
        }
    }

    private fun updateStatusesLabel(
        view: WPTextView,
        statuses: List<UiString>,
        delimiter: UiString,
        @ColorRes color: Int?
    ) {
        val separator = uiHelpers.getTextOfUiString(view.context, delimiter)
        view.text = statuses.joinToString(separator) { uiHelpers.getTextOfUiString(view.context, it) }
        color?.let { statusColor ->
            view.setTextColor(
                    ContextCompat.getColor(
                            itemView.context,
                            statusColor
                    )
            )
        }
    }

    private fun showFeaturedImage(imageUrl: String?) {
        if (!imageUrl.isNullOrBlank() && imageUrl == loadedFeaturedImgUrl) {
            // Suppress blinking as the media upload progresses
            return
        }
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
        loadedFeaturedImgUrl = imageUrl
    }
}
