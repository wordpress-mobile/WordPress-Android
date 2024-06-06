package org.wordpress.android.ui.reader.discover

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState.ReaderRecommendedBlogUiState
import org.wordpress.android.ui.reader.discover.viewholders.ReaderRecommendedBlogViewHolder
import org.wordpress.android.util.image.ImageManager

class ReaderRecommendedBlogsAdapter(
    private val imageManager: ImageManager,
) : ListAdapter<ReaderRecommendedBlogUiState, ReaderRecommendedBlogViewHolder>(RecommendedBlogsDiffUtil()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReaderRecommendedBlogViewHolder {
        return ReaderRecommendedBlogViewHolder(parent, imageManager)
    }

    override fun onBindViewHolder(holder: ReaderRecommendedBlogViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    class RecommendedBlogsDiffUtil : DiffUtil.ItemCallback<ReaderRecommendedBlogUiState>() {
        override fun areItemsTheSame(
            oldItem: ReaderRecommendedBlogUiState,
            newItem: ReaderRecommendedBlogUiState
        ): Boolean = oldItem.blogId == newItem.blogId && oldItem.feedId == newItem.feedId

        override fun areContentsTheSame(
            oldItem: ReaderRecommendedBlogUiState,
            newItem: ReaderRecommendedBlogUiState
        ): Boolean = oldItem == newItem

        // Returning true suppresses the default item animation.
        override fun getChangePayload(
            oldItem: ReaderRecommendedBlogUiState,
            newItem: ReaderRecommendedBlogUiState
        ): Any? = true
    }
}
