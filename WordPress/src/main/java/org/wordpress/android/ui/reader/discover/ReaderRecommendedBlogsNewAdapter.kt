package org.wordpress.android.ui.reader.discover

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState.ReaderRecommendedBlogUiState
import org.wordpress.android.ui.reader.discover.viewholders.ReaderRecommendedBlogNewViewHolder
import org.wordpress.android.util.image.ImageManager

class ReaderRecommendedBlogsNewAdapter(
    private val imageManager: ImageManager,
) : ListAdapter<ReaderRecommendedBlogUiState, ReaderRecommendedBlogNewViewHolder>(RecommendedBlogsDiffUtil()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReaderRecommendedBlogNewViewHolder {
        return ReaderRecommendedBlogNewViewHolder(parent, imageManager)
    }

    override fun onBindViewHolder(holder: ReaderRecommendedBlogNewViewHolder, position: Int) {
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
