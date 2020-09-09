package org.wordpress.android.ui.mediapicker

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams
import org.wordpress.android.R
import org.wordpress.android.widgets.WPTextView

class LoaderViewHolder(parent: ViewGroup) :
        ThumbnailViewHolder(parent, R.layout.media_picker_loader_item) {
    private val progress: View = itemView.findViewById(R.id.progress)
    private val error: WPTextView = itemView.findViewById(R.id.error)
    fun bind(item: MediaPickerUiItem.NextPageLoader) {
        setFullWidth()
        if (item.isLoading) {
            item.loadAction()
            progress.visibility = View.VISIBLE
            error.visibility = View.GONE
        } else if (item.error != null) {
            progress.visibility = View.GONE
            error.visibility = View.VISIBLE
            error.text = item.error
        }
    }

    private fun setFullWidth() {
        val layoutParams = itemView.layoutParams as? LayoutParams
        layoutParams?.isFullSpan = true
    }
}
