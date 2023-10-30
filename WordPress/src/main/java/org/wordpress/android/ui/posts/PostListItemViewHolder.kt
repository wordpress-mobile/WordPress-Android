package org.wordpress.android.ui.posts

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
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
import com.google.android.material.R as MaterialR

const val POST_LIST_ICON_PADDING = 8
const val MAX_TITLE_EXCERPT_LINES = 2

sealed class PostListItemViewHolder(
    @LayoutRes layout: Int,
    parent: ViewGroup,
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    private val featuredImageView: ImageView = itemView.findViewById(R.id.image_featured)
    private val titleTextView: TextView = itemView.findViewById(R.id.title)
    private val postInfoTextView: TextView = itemView.findViewById(R.id.post_info)
    private val statusesTextView: TextView = itemView.findViewById(R.id.statuses_label)
    private val uploadProgressBar: ProgressBar = itemView.findViewById(R.id.upload_progress)
    private val disabledOverlay: FrameLayout = itemView.findViewById(R.id.disabled_overlay)
    private val container: ConstraintLayout = itemView.findViewById(R.id.container)
    private val excerptTextView: TextView = itemView.findViewById(R.id.excerpt)
    private val moreButton: ImageView = itemView.findViewById(R.id.more)

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
            titleTextView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    // Remove the listener to avoid multiple callbacks
                    titleTextView.viewTreeObserver.removeOnPreDrawListener(this)

                    // Get the layout of the title text
                    val titleLayout = titleTextView.layout

                    // Check if the title occupies more than 2 lines
                    when (titleLayout.lineCount) {
                        1 -> {
                            excerptTextView.maxLines = 2
                        }
                        else -> {
                            excerptTextView.maxLines = 1
                            titleTextView.maxLines = 2
                        }
                    }
                    return true
                }
            })
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

    protected fun onMoreClicked(actions: List<PostListItemAction>, v: View) {
        val menu = PopupMenu(v.context, v)
        actions.forEach { singleItemAction ->
            val menuItem = menu.menu.add(
                singleItemAction.buttonType.groupId,
                singleItemAction.buttonType.value,
                Menu.NONE,
                getMenuItemTitleWithIcon(v.context, singleItemAction)
            )
            menuItem.setOnMenuItemClickListener {
                singleItemAction.onButtonClicked.invoke(singleItemAction.buttonType)
                true
            }
            MenuCompat.setGroupDividerEnabled(menu.menu, true)
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

    @Suppress("ComplexMethod")
    private fun getMenuItemTitleWithIcon(context: Context, item: PostListItemAction): SpannableStringBuilder {
        var icon: Drawable = setTint(
            context,
            ContextCompat.getDrawable(context, item.buttonType.iconResId)!!, item.buttonType.colorAttrId
        )

        // If there's no icon, we insert a transparent one to keep the title aligned with the items which have icons.
        if (item.buttonType.iconResId == 0) icon = ColorDrawable(Color.TRANSPARENT)

        val iconSize: Int = context.resources.getDimensionPixelSize(R.dimen.menu_item_icon_size)
        icon.setBounds(0, 0, iconSize, iconSize)
        val imageSpan = ImageSpan(icon)

        // Add a space placeholder for the icon, before the title.
        val menuTitle = context.getText(item.buttonType.textResId)

        // Use the same text color as the icon
        val textColor =
            if (item.buttonType.colorAttrId == 0) context.getColorFromAttribute(MaterialR.attr.colorOnSurface)
            else context.getColorFromAttribute(item.buttonType.colorAttrId)
        val ssb = SpannableStringBuilder(menuTitle.padStart(menuTitle.length + POST_LIST_ICON_PADDING))

        // Apply text color to the title.
        ssb.setSpan(
            ForegroundColorSpan(textColor),
            POST_LIST_ICON_PADDING,
            ssb.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Replace the space placeholder with the icon.
        ssb.setSpan(imageSpan, 1, 2, 0)
        return ssb
    }
}
