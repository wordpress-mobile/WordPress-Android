package org.wordpress.android.ui.mediapicker

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams
import org.wordpress.android.R

class LoaderViewHolder(parent: ViewGroup) :
    ThumbnailViewHolder(parent, R.layout.media_picker_loader_item) {
    private val progress: View = itemView.findViewById(R.id.progress)
    private val retry: Button = itemView.findViewById(R.id.button)
    fun bind(item: MediaPickerUiItem.NextPageLoader) {
        setFullWidth()
        if (item.isLoading) {
            item.loadAction()
            progress.visibility = View.VISIBLE
            retry.visibility = View.GONE
        } else {
            progress.visibility = View.GONE
            retry.visibility = View.VISIBLE
            retry.setOnClickListener {
                item.loadAction()
            }
        }
    }

    private fun setFullWidth() {
        val layoutParams = itemView.layoutParams as? LayoutParams
        layoutParams?.isFullSpan = true
    }
}
