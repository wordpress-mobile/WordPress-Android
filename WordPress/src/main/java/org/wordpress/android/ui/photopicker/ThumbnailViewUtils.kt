package org.wordpress.android.ui.photopicker

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.util.AccessibilityUtils
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration.SHORT
import org.wordpress.android.util.PhotoPickerUtils
import org.wordpress.android.util.ViewUtils
import org.wordpress.android.util.extensions.redirectContextClickToLongPressListener
import org.wordpress.android.util.image.ImageManager
import java.util.Locale

@Deprecated(
    "This class is being refactored, if you implement any change, please also update " +
            "{@link org.wordpress.android.ui.mediapicker.ThumbnailViewUtils}"
)
class ThumbnailViewUtils(val imageManager: ImageManager) {
    @Suppress("DEPRECATION", "LongParameterList")
    fun setupListeners(
        imgThumbnail: ImageView,
        isVideo: Boolean,
        isSelected: Boolean,
        toggleAction: PhotoPickerUiItem.ToggleAction,
        clickAction: PhotoPickerUiItem.ClickAction,
        animateSelection: Boolean
    ) {
        addImageSelectedToAccessibilityFocusedEvent(imgThumbnail, isSelected)
        imgThumbnail.setOnClickListener {
            toggleAction.toggle()
            PhotoPickerUtils.announceSelectedMediaForAccessibility(
                imgThumbnail,
                isVideo,
                !isSelected
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
                .getString(R.string.photo_picker_image_selected)
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
                        R.anim.pop
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
            txtSelectionCount.setBackgroundResource(R.drawable.photo_picker_circle_pressed)
        }
        displayTextSelectionCount(
            animateSelection,
            showOrderCounter,
            isSelected,
            txtSelectionCount
        )
    }

    @Suppress("DEPRECATION")
    fun setupVideoOverlay(videoOverlay: ImageView, clickAction: PhotoPickerUiItem.ClickAction) {
        videoOverlay.visibility = View.VISIBLE
        videoOverlay.setOnClickListener { clickAction.click() }
    }

    companion object {
        private const val SCALE_NORMAL = 1.0f
        private const val SCALE_SELECTED = .8f
        private val ANI_DURATION = SHORT
    }
}
