package org.wordpress.android.ui.sitecreation.theme

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageView.ScaleType.FIT_CENTER
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.home_page_picker_item.view.*
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageManager.RequestListener
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.util.setVisible

class HomePagePickerViewHolder(view: View, val parent: ViewGroup) : RecyclerView.ViewHolder(view) {
    private val container: View = itemView.layout_container
    private val preview: ImageView = itemView.preview
    private val selected: ImageView = itemView.selected_overlay

    fun onBind(
        uiState: LayoutGridItemUiState,
        imageManager: ImageManager
    ) {
        imageManager.loadWithResultListener(preview, ImageType.THEME, uiState.preview, FIT_CENTER, null,
                object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: Exception?, model: Any?) {
                    }

                    override fun onResourceReady(resource: Drawable, model: Any?) {
                        uiState.onThumbnailReady.invoke()
                    }
                })

        selected.setVisible(uiState.selectedOverlayVisible)
        preview.contentDescription = parent.context.getString(uiState.contentDescriptionResId, uiState.title)
        container.setOnClickListener {
            uiState.onItemTapped.invoke()
        }
    }
}
