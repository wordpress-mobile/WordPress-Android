package org.wordpress.android.ui.mlp

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import kotlinx.android.synthetic.main.modal_layout_picker_layouts_card.view.*
import org.wordpress.android.R
import org.wordpress.android.networking.MShot
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageManager.RequestListener
import org.wordpress.android.util.setVisible

/**
 * Renders the Layout card
 */
class LayoutViewHolder(internal val parent: ViewGroup) :
        RecyclerView.ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                        R.layout.modal_layout_picker_layouts_card,
                        parent,
                        false
                )
        ) {
    private val container: MaterialCardView = itemView.layout_container
    private val preview: ImageView = itemView.preview
    private val selected: ImageView = itemView.selected_overlay

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
