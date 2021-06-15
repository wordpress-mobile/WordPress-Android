package org.wordpress.android.ui.layoutpicker

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import org.wordpress.android.R
import org.wordpress.android.databinding.ModalLayoutPickerLayoutsCardBinding
import org.wordpress.android.networking.MShot
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageManager.RequestListener
import org.wordpress.android.util.setVisible
import org.wordpress.android.util.viewBinding

/**
 * Renders the Layout card
 */
class LayoutViewHolder(
    internal val parent: ViewGroup,
    internal val binding: ModalLayoutPickerLayoutsCardBinding =
            parent.viewBinding(ModalLayoutPickerLayoutsCardBinding::inflate)
) : RecyclerView.ViewHolder(binding.root) {
    private val container: MaterialCardView = binding.layoutContainer
    private val preview: ImageView = binding.preview
    private val selected: ImageView = binding.selectedOverlay

    fun onBind(
        uiState: LayoutListItemUiState,
        imageManager: ImageManager
    ) {
        imageManager.loadWithResultListener(preview, MShot(uiState.preview),
                object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: Exception?, model: Any?) {
                    }

                    override fun onResourceReady(resource: Drawable, model: Any?) {
                        uiState.onThumbnailReady.invoke()
                    }
                })

        selected.setVisible(uiState.selectedOverlayVisible)
        preview.contentDescription = parent.context.getString(uiState.contentDescriptionResId, uiState.title)
        preview.context?.let { ctx ->
            container.strokeWidth = if (uiState.selectedOverlayVisible) {
                ctx.resources.getDimensionPixelSize(R.dimen.picker_header_selection_overlay_width)
            } else {
                0
            }
        }
        container.setOnClickListener {
            uiState.onItemTapped.invoke()
        }
    }
}
