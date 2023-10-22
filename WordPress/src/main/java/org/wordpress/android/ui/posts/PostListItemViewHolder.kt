package org.wordpress.android.ui.posts

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageView.ScaleType
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
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.getDrawableFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.posts.PostInfo
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import org.wordpress.android.viewmodel.posts.PostListItemUiStateData
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState
import java.util.concurrent.atomic.AtomicBoolean
import android.R as AndroidR

const val POST_LIST_ICON_PADDING = 8
const val MAX_TITLE_LINES = 3

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
    private val selectableBackground: Drawable? = parent.context.getDrawableFromAttribute(
        AndroidR.attr.selectableItemBackground
    )

    private val noTitle = UiString.UiStringRes(R.string.untitled_in_parentheses)
    private val delimiter = " · "
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
            titleTextView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    // Remove the listener to avoid multiple callbacks
                    titleTextView.viewTreeObserver.removeOnPreDrawListener(this)

                    // Get the layout of the title text
                    val titleLayout = titleTextView.layout

                    // Check if the title occupies more than 3 lines
                    val titleLines = titleLayout.lineCount
                    if (titleLines >= MAX_TITLE_LINES) {
                        // If the title occupies more than 3 lines, hide the excerpt
                        excerptTextView.visibility = View.GONE
                    } else {
                        // If the title occupies 3 lines or less, show the excerpt
                        excerptTextView.visibility = View.VISIBLE
                    }
                    return true
                }
            })
        }
    }

    private fun updatePostInfoLabel(view: TextView, postInfoList: List<PostInfo>?) {
        if (!postInfoList.containsTrueShowColor()) {
            updatePostInfoWithoutColor(view, postInfoList?.map { it.label })
            return
        }

        val spannableStringBuilder = SpannableStringBuilder()

        postInfoList?.let { list ->
            list.forEachIndexed { index, postInfo ->
                if (index > 0) {
                    spannableStringBuilder.append(delimiter)
                }
                if (postInfo.showColor) {
                    spannableStringBuilder.append(getSpannableLabel(view, postInfo.label, postInfo.labelColor))
                } else {
                    spannableStringBuilder.append(uiHelpers.getTextOfUiString(view.context, postInfo.label))
                }
            }
        }
        uiHelpers.setTextOrHide(view, SpannableString.valueOf(spannableStringBuilder))
    }

    private fun updatePostInfoWithoutColor(view: TextView, uiStrings: List<UiString>?) {
        val concatenatedText = uiStrings?.joinToString(separator = delimiter) {
            uiHelpers.getTextOfUiString(view.context, it)
        }
        uiHelpers.setTextOrHide(view, concatenatedText)
    }

    private fun List<PostInfo>?.containsTrueShowColor(): Boolean {
        return this?.any { it.showColor } ?: false
    }


    // todo: Will come back to this when hooking up more actions
//    protected fun onMoreClicked(actions: List<PostListItemAction>, v: View) {
//        val menu = PopupMenu(v.context, v)
//        actions.forEach { singleItemAction ->
//            val menuItem = menu.menu.add(
//                Menu.NONE,
//                singleItemAction.buttonType.value,
//                Menu.NONE,
//                getMenuItemTitleWithIcon(v.context, singleItemAction)
//            )
//            menuItem.setOnMenuItemClickListener {
//                singleItemAction.onButtonClicked.invoke(singleItemAction.buttonType)
//                true
//            }
//        }
//        menu.show()
//    }

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
            imageManager.load(featuredImageView, ImageType.PHOTO, imageUrl, ScaleType.CENTER_CROP)
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

    // todo: Will come back to this when hooking up more actions
//    @Suppress("ComplexMethod")
//    private fun getMenuItemTitleWithIcon(context: Context, item: PostListItemAction): SpannableStringBuilder {
//        var icon: Drawable? = setTint(
//            context,
//            ContextCompat.getDrawable(context, item.buttonType.iconResId)!!, item.buttonType.colorAttrId
//        )
//        // If there's no icon, we insert a transparent one to keep the title aligned with the items which have icons.
//        if (icon == null) icon = ColorDrawable(Color.TRANSPARENT)
//        val iconSize: Int = context.getResources().getDimensionPixelSize(R.dimen.menu_item_icon_size)
//        icon.setBounds(0, 0, iconSize, iconSize)
//        val imageSpan = ImageSpan(icon)
//
//        // Add a space placeholder for the icon, before the title.
//        val menuTitle = context.getText(item.buttonType.textResId)
//        val ssb = SpannableStringBuilder(
//            menuTitle.padStart(menuTitle.length + POST_LIST_ICON_PADDING)
//        )
//
//        // Replace the space placeholder with the icon.
//        ssb.setSpan(imageSpan, 1, 2, 0)
//        return ssb
//    }
    private fun getSpannableLabel(view: TextView, label: UiString, colorResId: Int): SpannableString {
        val originalText = uiHelpers.getTextOfUiString(view.context, label)
        val spannableString = SpannableString(originalText)

        // Set the color for a specific range of characters
        val color = view.context.getColor(colorResId)
        val colorSpan = ForegroundColorSpan(color)
        spannableString.setSpan(colorSpan, 0, originalText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return spannableString
    }
}
