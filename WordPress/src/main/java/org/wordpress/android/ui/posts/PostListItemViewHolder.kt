package org.wordpress.android.ui.posts

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
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.core.view.MenuCompat
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.utils.UiDimen
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.extensions.expandTouchTargetArea
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.getDrawableFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.posts.PostListItemAction
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import org.wordpress.android.viewmodel.posts.PostListItemUiStateData
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState
import java.util.concurrent.atomic.AtomicBoolean
import android.R as AndroidR
import androidx.appcompat.widget.PopupMenu as AppCompatPopupMenu
import com.google.android.material.R as MaterialR

const val MAX_TITLE_EXCERPT_LINES = 2

sealed class PostListItemViewHolder(
    @LayoutRes layout: Int,
    parent: ViewGroup,
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    private val featuredImageView: ImageView = itemView.findViewById(R.id.image_featured)
    private val titleTextView: PostListTitleExcerptTextView = itemView.findViewById(R.id.title)
    private val postInfoTextView: TextView = itemView.findViewById(R.id.post_info)
    private val statusesTextView: TextView = itemView.findViewById(R.id.statuses_label)
    private val uploadProgressBar: ProgressBar = itemView.findViewById(R.id.upload_progress)
    private val disabledOverlay: FrameLayout = itemView.findViewById(R.id.disabled_overlay)
    private val container: ConstraintLayout = itemView.findViewById(R.id.container)
    private val excerptTextView: PostListTitleExcerptTextView = itemView.findViewById(R.id.excerpt)
    private val moreButton: ImageButton = itemView.findViewById(R.id.more)

    private val selectableBackground: Drawable? = parent.context.getDrawableFromAttribute(
        AndroidR.attr.selectableItemBackground
    )

    private val noTitle = UiString.UiStringRes(R.string.untitled_in_parentheses)
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
        uiHelpers: UiHelpers
    ) : PostListItemViewHolder(R.layout.post_list_item, parent, imageManager, uiHelpers) {
        override fun onBind(item: PostListItemUiState) {
            setBasicValues(item.data)
            setMoreActions(item.moreActions.actions)

            itemView.setOnClickListener {
                if (isSafeClick(it)) {
                    item.onSelected.invoke()
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

    protected fun setBasicValues(data: PostListItemUiStateData) {
        uiHelpers.setTextOrHide(titleTextView, data.title)
        uiHelpers.setTextOrHide(excerptTextView, data.excerpt)
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

        if ((data.title != null && data.title != noTitle) && data.excerpt != null) {
            titleTextView.maxLines = MAX_TITLE_EXCERPT_LINES
            excerptTextView.maxLines = MAX_TITLE_EXCERPT_LINES
            titleTextView.setTargetTextView(titleTextView, PostListTitleExcerptTextView.CallingTextView.TITLE)
            excerptTextView.setTargetTextView(titleTextView, PostListTitleExcerptTextView.CallingTextView.EXCERPT)
        }
    }

    protected fun setMoreActions(actions: List<PostListItemAction>) {
        moreButton.expandTouchTargetArea(R.dimen.post_list_more_button_extra_padding)
        moreButton.setOnClickListener { onMoreClicked(actions, moreButton) }
    }

    private fun updatePostInfoLabel(view: TextView, uiStrings: List<UiString>?) {
        val concatenatedText = uiStrings?.joinToString(separator = "  ·  ") {
            uiHelpers.getTextOfUiString(view.context, it)
        }
        uiHelpers.setTextOrHide(view, concatenatedText)
    }

    private fun onMoreClicked(actions: List<PostListItemAction>, v: View) {
        val emptyDrawable = ContextCompat.getDrawable(v.context, R.drawable.ic_placeholder_24dp)
        val menu = AppCompatPopupMenu(v.context, v, GravityCompat.END)
        MenuCompat.setGroupDividerEnabled(menu.menu, true)
        menu.setForceShowIcon(true)
        actions.forEach { singleItemAction ->
            val menuItem = menu.menu.add(
                singleItemAction.buttonType.groupId,
                singleItemAction.buttonType.value,
                Menu.NONE,
                singleItemAction.buttonType.textResId
            )
            menuItem.setOnMenuItemClickListener {
                singleItemAction.onButtonClicked.invoke(singleItemAction.buttonType)
                true
            }

            setIconAndIconColorIfNeeded(v.context, menuItem, singleItemAction, emptyDrawable)
            setTextColorIfNeeded(v.context, menuItem, singleItemAction)
        }
        menu.show()
    }

    private fun setIconAndIconColorIfNeeded(
        context: Context,
        menuItem: MenuItem,
        singleItemAction: PostListItemAction,
        emptyDrawable: Drawable?
    ) {
        if (singleItemAction.buttonType.iconResId > 0) {
            val icon: Drawable = setTint(
                context,
                ContextCompat.getDrawable(context, singleItemAction.buttonType.iconResId)!!,
                singleItemAction.buttonType.colorAttrId
            )
            menuItem.icon = icon
        } else {
            // Leave space for the icon so the text lines up
            menuItem.icon = emptyDrawable
        }
    }

    private fun setTextColorIfNeeded(context: Context,
                                     menuItem: MenuItem,
                                     singleItemAction: PostListItemAction) {
        if (singleItemAction.buttonType.colorAttrId > 0 &&
            singleItemAction.buttonType.colorAttrId != MaterialR.attr.colorOnSurface) {
            val menuTitle = context.getText(singleItemAction.buttonType.textResId)
            val spannableString = SpannableString(menuTitle)
            val textColor = context.getColorFromAttribute(singleItemAction.buttonType.colorAttrId)

            spannableString.setSpan(
                ForegroundColorSpan(textColor),
                0,
                spannableString.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )

            menuItem.title = spannableString
        }
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
        view: TextView,
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
            imageManager.loadImageWithCorners(
                featuredImageView,
                ImageType.PHOTO_ROUNDED_CORNERS,
                imageUrl,
                uiHelpers.getPxOfUiDimen(
                    WordPress.getContext(),
                    UiDimen.UIDimenRes(R.dimen.my_site_post_item_image_corner_radius)
                )
            )
        }
        loadedFeaturedImgUrl = imageUrl
    }

    private fun setTint(context: Context, drawable: Drawable, color: Int): Drawable {
        val wrappedDrawable: Drawable = DrawableCompat.wrap(drawable)
        if (color != 0) {
            val iconColor = context.getColorFromAttribute(color)
            DrawableCompat.setTint(wrappedDrawable, iconColor)
        }
        return wrappedDrawable
    }
}
