package org.wordpress.android.ui.posts

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
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
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.extensions.expandTouchTargetArea
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.getDrawableFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.posts.PostListItemAction
import org.wordpress.android.viewmodel.posts.PostListItemAction.MoreItem
import org.wordpress.android.viewmodel.posts.PostListItemAction.SingleItem
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import org.wordpress.android.viewmodel.posts.PostListItemUiStateData
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState
import org.wordpress.android.widgets.PostListButton
import org.wordpress.android.widgets.WPTextView
import java.util.concurrent.atomic.AtomicBoolean

sealed class PostListItemViewHolder(
    @LayoutRes layout: Int,
    parent: ViewGroup,
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    private val featuredImageView: ImageView = itemView.findViewById(R.id.image_featured)
    private val titleTextView: WPTextView = itemView.findViewById(R.id.title)
    private val postInfoTextView: WPTextView = itemView.findViewById(R.id.post_info)
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

    companion object {
        var isClickEnabled = AtomicBoolean(true)
    }

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
            itemView.setOnClickListener {
                if (isSafeClick(it)) {
                    item.onSelected.invoke()
                }
            }

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
                        postListButton.setOnClickListener {
                            if (isSafeClick(it)) {
                                action.onButtonClicked.invoke(action.buttonType)
                            }
                        }
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

        // Purpose of this method is to prevent 2 Editors
        // from being launched simultaneously and then producing a crash
        private fun isSafeClick(view: View): Boolean {
            if (isClickEnabled.getAndSet(false)) {
                view.postDelayed({
                    isClickEnabled.set(true)
                }, 1000)
                return true
            }
            return false
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
        updatePostInfoLabel(postInfoTextView, data.postInfo)
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

    private fun updatePostInfoLabel(view: TextView, uiStrings: List<UiString>?) {
        val concatenatedText = uiStrings?.joinToString(separator = "  ·  ") {
            uiHelpers.getTextOfUiString(view.context, it)
        }
        uiHelpers.setTextOrHide(view, concatenatedText)
    }

    protected fun onMoreClicked(actions: List<PostListItemAction>, v: View) {
        val menu = PopupMenu(v.context, v)
        actions.forEach { singleItemAction ->
            val menuItem = menu.menu.add(
                Menu.NONE,
                singleItemAction.buttonType.value,
                Menu.NONE,
                getMenuItemTitleWithIcon(v.context, singleItemAction)
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
            is ProgressBarUiState.Indeterminate -> uploadProgressBar.isIndeterminate = true
            is ProgressBarUiState.Determinate -> {
                uploadProgressBar.isIndeterminate = false
                uploadProgressBar.progress = progressBarUiState.progress
            }
            is ProgressBarUiState.Hidden -> Unit // Do nothing
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

    private fun setTint(context: Context , drawable: Drawable, color: Int): Drawable {
        val wrappedDrawable: Drawable = DrawableCompat.wrap(drawable)
        if(color != 0) {
            val iconColor = context.getColorFromAttribute(color)
            DrawableCompat.setTint(wrappedDrawable, iconColor)
        }
        return wrappedDrawable
    }

    @Suppress("ComplexMethod")
    private fun getMenuItemTitleWithIcon(context: Context, item: PostListItemAction) : SpannableStringBuilder {
        var icon: Drawable? = setTint(context,
            context.getDrawable(item.buttonType.iconResId)!!,item.buttonType.colorAttrId)
        // If there's no icon, we insert a transparent one to keep the title aligned with the items which have icons.
        if (icon == null) icon = ColorDrawable(Color.TRANSPARENT)
        val iconSize: Int = context.getResources().getDimensionPixelSize(R.dimen.menu_item_icon_size)
        icon.setBounds(0, 0, iconSize, iconSize)
        val imageSpan = ImageSpan(icon)

        // Add a space placeholder for the icon, before the title.
        val ssb = SpannableStringBuilder("       " + context.getText(item.buttonType.textResId))

        // Replace the space placeholder with the icon.
        ssb.setSpan(imageSpan, 1, 2, 0)
        return ssb
    }
}
