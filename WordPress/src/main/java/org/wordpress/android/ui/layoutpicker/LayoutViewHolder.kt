package org.wordpress.android.ui.layoutpicker

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.databinding.ModalLayoutPickerLayoutsCardBinding
import org.wordpress.android.networking.MShot
import org.wordpress.android.util.extensions.setVisible
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageManager.RequestListener

/**
 * Renders the Layout card
 */
class LayoutViewHolder(
    private val parent: ViewGroup,
    private val binding: ModalLayoutPickerLayoutsCardBinding,
    private val thumbDimensionProvider: ThumbDimensionProvider
) : RecyclerView.ViewHolder(binding.root) {
    fun onBind(
        uiState: LayoutListItemUiState,
        imageManager: ImageManager
    ) {
        imageManager.loadWithResultListener(binding.preview, MShot(uiState.mShotPreview),
            object : RequestListener<Drawable> {
                override fun onLoadFailed(e: Exception?, model: Any?) {
                }

                override fun onResourceReady(resource: Drawable, model: Any?) {
                    uiState.onThumbnailReady.invoke()
                }
            })

        binding.selectedOverlay.setVisible(uiState.selectedOverlayVisible)
        binding.preview.updateLayoutParams {
            height = thumbDimensionProvider.previewHeight
            width = thumbDimensionProvider.previewWidth
        }
        binding.preview.contentDescription = parent.context.getString(uiState.contentDescriptionResId, uiState.title)
        binding.preview.context?.let { ctx ->
            binding.layoutContainer.strokeWidth = if (uiState.selectedOverlayVisible) {
                ctx.resources.getDimensionPixelSize(R.dimen.picker_header_selection_overlay_width)
            } else {
                0
            }
        }
        binding.layoutContainer.setOnClickListener {
            uiState.onItemTapped.invoke()
        }
    }

    companion object {
        fun from(parent: ViewGroup, thumbDimensionProvider: ThumbDimensionProvider): LayoutViewHolder {
            val binding = ModalLayoutPickerLayoutsCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return LayoutViewHolder(parent, binding, thumbDimensionProvider)
        }
    }
}
