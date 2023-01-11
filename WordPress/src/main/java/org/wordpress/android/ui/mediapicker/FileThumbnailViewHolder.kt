package org.wordpress.android.ui.mediapicker

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R

class FileThumbnailViewHolder(parent: ViewGroup, private val mediaThumbnailViewUtils: MediaThumbnailViewUtils) :
    ThumbnailViewHolder(parent, R.layout.media_picker_file_item) {
    private val container: View = itemView.findViewById(R.id.media_grid_item_file_container)
    private val imgThumbnail: ImageView = itemView.findViewById(R.id.media_item_filetype_image)
    private val fileType: TextView = itemView.findViewById(R.id.media_item_filetype)
    private val fileName: TextView = itemView.findViewById(R.id.media_item_name)
    private val txtSelectionCount: TextView = itemView.findViewById(R.id.text_selection_count)

    fun bind(item: MediaPickerUiItem.FileItem, animateSelection: Boolean, updateCount: Boolean) {
        val isSelected = item.isSelected
        mediaThumbnailViewUtils.setupTextSelectionCount(
            txtSelectionCount,
            isSelected,
            item.selectedOrder,
            item.showOrderCounter,
            animateSelection
        )
        // Only count is updated so do not redraw the whole item
        if (updateCount) {
            return
        }
        mediaThumbnailViewUtils.setupFileImageView(
            container,
            imgThumbnail,
            item.fileName,
            item.isSelected,
            item.clickAction,
            item.toggleAction,
            animateSelection
        )
        fileType.text = item.fileExtension
        fileName.text = item.fileName
    }
}
