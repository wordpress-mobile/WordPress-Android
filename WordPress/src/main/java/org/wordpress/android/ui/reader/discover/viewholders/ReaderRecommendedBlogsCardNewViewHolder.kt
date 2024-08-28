package org.wordpress.android.ui.reader.discover.viewholders

import android.view.ViewGroup
import org.wordpress.android.databinding.ReaderRecommendedBlogsCardBinding
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState
import org.wordpress.android.ui.reader.discover.ReaderRecommendedBlogsAdapter
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager

class ReaderRecommendedBlogsCardNewViewHolder(
    parentView: ViewGroup,
    imageManager: ImageManager,
) : ReaderViewHolder<ReaderRecommendedBlogsCardBinding>(
    parentView.viewBinding(ReaderRecommendedBlogsCardBinding::inflate)
) {
    private val recommendedBlogsAdapter =
        ReaderRecommendedBlogsAdapter(imageManager)

    init {
        with(binding) {
            recommendedBlogs.adapter = recommendedBlogsAdapter
        }
    }

    override fun onBind(uiState: ReaderCardUiState) {
        uiState as ReaderRecommendedBlogsCardUiState
        recommendedBlogsAdapter.submitList(uiState.blogs)
    }
}
