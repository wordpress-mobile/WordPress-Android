package org.wordpress.android.ui.mlp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageView.ScaleType.FIT_CENTER
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.modal_layout_picker_layouts_card.view.*
import org.wordpress.android.R
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
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
    private val container: View = itemView.layout_container
    private val preview: ImageView = itemView.preview
    private val selected: ImageView = itemView.selected_overlay

    fun onBind(
        uiState: LayoutListItemUiState,
        imageManager: ImageManager
    ) {
        imageManager.load(preview, ImageType.THEME, uiState.preview, FIT_CENTER)
        selected.setVisible(uiState.selected)
        preview.contentDescription = if (uiState.selected) parent.context.getString(
                R.string.mlp_layout_selected,
                uiState.title
        ) else uiState.title
        container.setOnClickListener {
            uiState.onItemTapped.invoke()
        }
    }
}
