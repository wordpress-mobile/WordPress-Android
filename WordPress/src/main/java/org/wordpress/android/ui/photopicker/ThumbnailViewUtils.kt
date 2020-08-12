package org.wordpress.android.ui.photopicker

import android.view.View
import android.widget.ImageView
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.TextView
import org.wordpress.android.R.anim
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.ui.photopicker.PhotoPickerUiItem.ClickAction
import org.wordpress.android.ui.photopicker.PhotoPickerUiItem.ToggleAction
import org.wordpress.android.util.AccessibilityUtils
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration.SHORT
import org.wordpress.android.util.PhotoPickerUtils
import org.wordpress.android.util.ViewUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.PHOTO
import org.wordpress.android.util.redirectContextClickToLongPressListener
import java.util.Locale

class ThumbnailViewUtils(val imageManager: ImageManager) {
    fun setupThumbnailImage(
        imgThumbnail: ImageView,
        url: String,
        isSelected: Boolean,
        clickAction: ClickAction,
        toggleAction: ToggleAction,
        animateSelection: Boolean
    ) {
        imageManager.cancelRequestAndClearImageView(imgThumbnail)
        imageManager.load(
                imgThumbnail,
                PHOTO,
                url,
                FIT_CENTER
        )
        addImageSelectedToAccessibilityFocusedEvent(imgThumbnail, isSelected)
        imgThumbnail.setOnClickListener {
            toggleAction.toggle()
            PhotoPickerUtils.announceSelectedImageForAccessibility(
                    imgThumbnail,
                    isSelected
            )
        }
        imgThumbnail.setOnLongClickListener {
            clickAction.click()
            true
        }
        imgThumbnail.redirectContextClickToLongPressListener()
        displaySelection(animateSelection, isSelected, imgThumbnail)
    }

    private fun addImageSelectedToAccessibilityFocusedEvent(
        imageView: ImageView,
        isSelected: Boolean
    ) {
        AccessibilityUtils.addPopulateAccessibilityEventFocusedListener(
                imageView
        ) {
            val imageSelectedText = imageView.context
                    .getString(string.photo_picker_image_selected)
            if (isSelected) {
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

    private fun displaySelection(animate: Boolean, isSelected: Boolean, imageView: ImageView) {
        if (animate) {
            if (isSelected) {
                AniUtils.scale(
                        imageView,
                        SCALE_NORMAL,
                        SCALE_SELECTED,
                        ANI_DURATION
                )
            } else {
                AniUtils.scale(
                        imageView,
                        SCALE_SELECTED,
                        SCALE_NORMAL,
                        ANI_DURATION
                )
            }
        } else {
            val scale = if (isSelected) SCALE_SELECTED else SCALE_NORMAL
            if (imageView.scaleX != scale) {
                imageView.scaleX = scale
                imageView.scaleY = scale
            }
        }
    }

    fun displayTextSelectionCount(
        animate: Boolean,
        showOrderCounter: Boolean,
        isSelected: Boolean,
        txtSelectionCount: TextView
    ) {
        if (animate) {
            when {
                showOrderCounter -> {
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
            txtSelectionCount.visibility = if (showOrderCounter || isSelected) View.VISIBLE else View.GONE
        }
    }

    fun updateSelectionCountForPosition(
        txtSelectionCount: TextView,
        selectedOrder: Int?
    ) {
        if (selectedOrder != null) {
            txtSelectionCount.text = String.format(Locale.getDefault(), "%d", selectedOrder)
        } else {
            txtSelectionCount.text = null
        }
    }

    fun setupTextSelectionCount(
        txtSelectionCount: TextView,
        isSelected: Boolean,
        selectedOrder: Int?,
        showOrderCounter: Boolean,
        animateSelection: Boolean
    ) {
        ViewUtils.addCircularShadowOutline(
                txtSelectionCount
        )
        txtSelectionCount.isSelected = isSelected
        updateSelectionCountForPosition(txtSelectionCount, selectedOrder)
        if (!showOrderCounter) {
            txtSelectionCount.setBackgroundResource(drawable.photo_picker_circle_pressed)
        }
        displayTextSelectionCount(
                animateSelection,
                showOrderCounter,
                isSelected,
                txtSelectionCount
        )
    }

    fun setupVideoOverlay(videoOverlay: ImageView, clickAction: ClickAction) {
        videoOverlay.visibility = View.VISIBLE
        videoOverlay.setOnClickListener { clickAction.click() }
    }

    companion object {
        private const val SCALE_NORMAL = 1.0f
        private const val SCALE_SELECTED = .8f
        private val ANI_DURATION = SHORT
    }
}
