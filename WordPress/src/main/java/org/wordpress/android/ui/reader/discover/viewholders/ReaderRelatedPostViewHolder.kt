package org.wordpress.android.ui.reader.discover.viewholders

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ReaderCardviewRelatedPostBinding
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState.RelatedPostsUiState.ReaderRelatedPostUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.PHOTO
import org.wordpress.android.util.extensions.viewBinding

class ReaderRelatedPostViewHolder(
    private val uiHelpers: UiHelpers,
    private val imageManager: ImageManager,
    private val parent: ViewGroup,
    private val binding: ReaderCardviewRelatedPostBinding =
            parent.viewBinding(ReaderCardviewRelatedPostBinding::inflate)
) : RecyclerView.ViewHolder(binding.root) {
    fun onBind(state: ReaderRelatedPostUiState) = with(binding) {
        updateFeaturedImage(state)
        uiHelpers.setTextOrHide(textTitle, state.title)
        uiHelpers.setTextOrHide(textExcerpt, state.excerpt)
        itemView.setOnClickListener { state.onItemClicked.invoke(state.postId, state.blogId, state.isGlobal) }
    }

    private fun updateFeaturedImage(state: ReaderRelatedPostUiState) = with(binding) {
        uiHelpers.updateVisibility(imageFeatured, state.featuredImageVisibility)
        if (state.featuredImageUrl == null) {
            imageManager.cancelRequestAndClearImageView(imageFeatured)
        } else {
            imageManager.loadImageWithCorners(
                    imageFeatured,
                    PHOTO,
                    state.featuredImageUrl,
                    uiHelpers.getPxOfUiDimen(WordPress.getContext(), state.featuredImageCornerRadius)
            )
        }
    }
}
