package org.wordpress.android.ui.reader.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.ReaderCardUiState

sealed class ReaderViewHolder(internal val parent: ViewGroup, @LayoutRes layout: Int) :
        RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(uiState: ReaderCardUiState)

    class ReaderPostViewHolder(
        parentView: ViewGroup
    ) : ReaderViewHolder(parentView, R.layout.reader_cardview_post) {
        override fun onBind(uiState: ReaderCardUiState) {
        }
    }
}
