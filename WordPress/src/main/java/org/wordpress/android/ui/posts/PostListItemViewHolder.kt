package org.wordpress.android.ui.posts

import android.graphics.drawable.Drawable
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.expandTouchTargetArea
import org.wordpress.android.util.getDrawableFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.posts.PostListItemAction
import org.wordpress.android.viewmodel.posts.PostListItemAction.MoreItem
import org.wordpress.android.viewmodel.posts.PostListItemAction.SingleItem
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState.Determinate
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState.Indeterminate
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import org.wordpress.android.viewmodel.posts.PostListItemUiStateData
import org.wordpress.android.widgets.PostListButton
import org.wordpress.android.widgets.WPTextView

sealed class PostListItemViewHolder(
    @LayoutRes layout: Int,
    parent: ViewGroup,
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    private val featuredImageView: ImageView = itemView.findViewById(R.id.image_featured)
    private val titleTextView: WPTextView = itemView.findViewById(R.id.title)
    private val dateAndAuthorTextView: WPTextView = itemView.findViewById(R.id.date_and_author)
    private val statusesTextView: WPTextView = itemView.findViewById(R.id.statuses_label)
    private val uploadProgressBar: ProgressBar = itemView.findViewById(R.id.upload_progress)
    private val disabledOverlay: FrameLayout = itemView.findViewById(R.id.disabled_overlay)
    private val container: ConstraintLayout = itemView.findViewById(R.id.container)
    private val selectableBackground: Drawable? = parent.context.getDrawableFromAttribute(
            android.R.attr.selectableItemBackground
    )
    /**
     * Url of an image loaded in the `featuredImageView`.
     */
    private var loadedFeaturedImgUrl: String? = null

    abstract fun onBind(item: PostListItemUiState)

    class Standard(
        parent: ViewGroup,
        imageManager: ImageManager,
        private val uiHelpers: UiHelpers
    ) : PostListItemViewHolder(R.layout.post_list_item, parent, imageManager, uiHelpers) {
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
        imageManager: ImageManager,
        private val uiHelpers: UiHelpers
    ) : PostListItemViewHolder(R.layout.post_list_item_compact, parent, imageManager, uiHelpers) {
        private val moreButton: ImageButton = itemView.findViewById(R.id.more_button)

        override fun onBind(item: PostListItemUiState) {
            setBasicValues(item.data)

            itemView.setOnClickListener { item.onSelected.invoke() }
            uiHelpers.updateVisibility(moreButton, item.compactActions.actions.isNotEmpty())
            moreButton.expandTouchTargetArea(R.dimen.post_list_more_button_extra_padding)
            moreButton.setOnClickListener { onMoreClicked(item.compactActions.actions, moreButton) }
        }
    }

    protected fun setBasicValues(data: PostListItemUiStateData) {
        uiHelpers.setTextOrHide(titleTextView, data.title)
        uiHelpers.setTextOrHide(dateAndAuthorTextView, data.dateAndAuthor)
        uiHelpers.updateVisibility(statusesTextView, data.statuses.isNotEmpty())
        updateStatusesLabel(statusesTextView, data.statuses, data.statusesDelimiter, data.statusesColor)
        showFeaturedImage(data.imageUrl)
        updateProgressBarState(data.progressBarUiState)
        uiHelpers.updateVisibility(disabledOverlay, data.showOverlay)
        if (data.disableRippleEffect) {
            container.background = null
        } else {
            container.background = selectableBackground
        }
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

    private fun updateProgressBarState(progressBarUiState: ProgressBarUiState) {
        uiHelpers.updateVisibility(uploadProgressBar, progressBarUiState.visibility)
        when (progressBarUiState) {
            Indeterminate -> uploadProgressBar.isIndeterminate = true
            is Determinate -> {
                uploadProgressBar.isIndeterminate = false
                uploadProgressBar.progress = progressBarUiState.progress
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
        if (imageUrl.isNullOrBlank()) {
            featuredImageView.visibility = View.GONE
            imageManager.cancelRequestAndClearImageView(featuredImageView)
        } else {
            featuredImageView.visibility = View.VISIBLE
            imageManager.load(featuredImageView, ImageType.PHOTO, imageUrl, ScaleType.CENTER_CROP)
        }
        loadedFeaturedImgUrl = imageUrl
    }
}
