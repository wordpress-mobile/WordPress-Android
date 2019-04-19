package org.wordpress.android.ui.posts

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.PopupMenu
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import org.wordpress.android.R
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.posts.PostListItemAction
import org.wordpress.android.viewmodel.posts.PostListItemUiData
import org.wordpress.android.widgets.WPTextView

abstract class PostListItemSharedViewHolder(
    @LayoutRes layout: Int,
    parent: ViewGroup,
    private val config: PostViewHolderConfig,
    private val uiHelpers: UiHelpers
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    private val featuredImageView: ImageView = itemView.findViewById(R.id.image_featured)
    private val titleTextView: WPTextView = itemView.findViewById(R.id.title)
    private val dateTextView: WPTextView = itemView.findViewById(R.id.date)
    private val statusesTextView: WPTextView = itemView.findViewById(R.id.statuses_label)

    protected fun setBasicValues(data: PostListItemUiData) {
        uiHelpers.setTextOrHide(titleTextView, data.title)
        uiHelpers.setTextOrHide(dateTextView, data.date)
        uiHelpers.updateVisibility(statusesTextView, data.statuses.isNotEmpty())
        updateStatusesLabel(statusesTextView, data.statuses, data.statusesDelimiter, data.statusesColor)
        showFeaturedImage(data.imageUrl)
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

    private fun updateStatusesLabel(
        view: WPTextView, statuses: List<UiString>,
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