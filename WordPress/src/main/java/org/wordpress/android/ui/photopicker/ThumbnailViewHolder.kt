package org.wordpress.android.ui.photopicker

import android.view.View
import android.widget.ImageView
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.R.anim
import org.wordpress.android.R.drawable
import org.wordpress.android.R.id
import org.wordpress.android.R.string
import org.wordpress.android.util.AccessibilityUtils
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration.SHORT
import org.wordpress.android.util.PhotoPickerUtils
import org.wordpress.android.util.ViewUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.PHOTO
import org.wordpress.android.util.redirectContextClickToLongPressListener
import java.util.Locale

/*
 * ViewHolder containing a device thumbnail
 */
class ThumbnailViewHolder(view: View, val imageManager: ImageManager) : ViewHolder(view) {
    private val imgThumbnail: ImageView = view.findViewById(id.image_thumbnail)
    private val txtSelectionCount: TextView = view.findViewById(id.text_selection_count)
    private val videoOverlay: ImageView = view.findViewById(id.image_video_overlay)

    private fun addImageSelectedToAccessibilityFocusedEvent(
        imageView: ImageView,
        item: PhotoPickerUiItem
    ) {
        AccessibilityUtils.addPopulateAccessibilityEventFocusedListener(
                imageView
        ) {
            val imageSelectedText = imageView.context
                    .getString(string.photo_picker_image_selected)
            if (item.isSelected) {
                if (!imageView.contentDescription.toString().contains(imageSelectedText)) {
                    imageView.contentDescription = ("${imageView.contentDescription} $imageSelectedText")
                }
            } else {
                imageView.contentDescription = imageView.contentDescription
                        .toString().replace(
                                imageSelectedText,
                                ""
                        )
            }
        }
    }

    fun bind(item: PhotoPickerUiItem, animateSelection: Boolean, updateCount: Boolean) {
        // Only count is updated so do not redraw the whole item
        if (updateCount) {
            updateSelectionCountForPosition(item, txtSelectionCount)
            AniUtils.startAnimation(
                    txtSelectionCount,
                    anim.pop
            )
            return
        }
        ViewUtils.addCircularShadowOutline(
                txtSelectionCount
        )
        val isSelected = item.isSelected
        txtSelectionCount.isSelected = isSelected
        updateSelectionCountForPosition(item, txtSelectionCount)
        videoOverlay.visibility = if (item.isVideo) View.VISIBLE else View.GONE
        if (!item.showOrderCounter) {
            txtSelectionCount.setBackgroundResource(drawable.photo_picker_circle_pressed)
        }
        imageManager.cancelRequestAndClearImageView(imgThumbnail)
        imageManager.load(
                imgThumbnail,
                PHOTO,
                item.uri.toString(),
                FIT_CENTER
        )
        addImageSelectedToAccessibilityFocusedEvent(imgThumbnail, item)
        imgThumbnail.setOnClickListener {
            item.toggleAction.toggle()
            PhotoPickerUtils.announceSelectedImageForAccessibility(
                    imgThumbnail,
                    item.isSelected
            )
        }
        imgThumbnail.setOnLongClickListener {
            item.clickAction.click()
            true
        }
        imgThumbnail.redirectContextClickToLongPressListener()
        videoOverlay.setOnClickListener { item.clickAction.click() }
        if (animateSelection) {
            if (isSelected) {
                AniUtils.scale(
                        imgThumbnail,
                        SCALE_NORMAL,
                        SCALE_SELECTED,
                        ANI_DURATION
                )
            } else {
                AniUtils.scale(
                        imgThumbnail,
                        SCALE_SELECTED,
                        SCALE_NORMAL,
                        ANI_DURATION
                )
            }
            when {
                item.showOrderCounter -> {
                    AniUtils.startAnimation(
                            txtSelectionCount,
                            anim.pop
                    )
                }
                isSelected -> {
                    AniUtils.fadeIn(
                            txtSelectionCount,
                            ANI_DURATION
                    )
                }
                else -> {
                    AniUtils.fadeOut(
                            txtSelectionCount,
                            ANI_DURATION
                    )
                }
            }
        } else {
            val scale = if (isSelected) SCALE_SELECTED else SCALE_NORMAL
            if (imgThumbnail.scaleX != scale) {
                imgThumbnail.scaleX = scale
                imgThumbnail.scaleY = scale
            }
            txtSelectionCount.visibility = if (item.showOrderCounter || isSelected) View.VISIBLE else View.GONE
        }
    }

    private fun updateSelectionCountForPosition(
        item: PhotoPickerUiItem,
        txtSelectionCount: TextView
    ) {
        if (item.selectedOrder != null) {
            txtSelectionCount.text = String.format(Locale.getDefault(), "%d", item.selectedOrder)
        } else {
            txtSelectionCount.text = null
        }
    }

    companion object {
        private const val SCALE_NORMAL = 1.0f
        private const val SCALE_SELECTED = .8f
        private val ANI_DURATION = SHORT
    }
}
