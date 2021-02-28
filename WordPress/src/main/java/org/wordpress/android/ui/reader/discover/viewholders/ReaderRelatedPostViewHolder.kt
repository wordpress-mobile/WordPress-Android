package org.wordpress.android.ui.reader.discover.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.reader_cardview_related_post.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.ReaderPostDetailsUiState.RelatedPostsUiState.ReaderRelatedPostUiState

import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.PHOTO
import org.wordpress.android.util.setVisible

class ReaderRelatedPostViewHolder(
    private val uiHelpers: UiHelpers,
    private val imageManager: ImageManager,
    private val parent: ViewGroup,
    override val containerView: View = LayoutInflater.from(parent.context).inflate(
            R.layout.reader_cardview_related_post,
            parent,
            false
    )
) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun onBind(state: ReaderRelatedPostUiState) {
        updateFeaturedImage(state)
        updateFollowButton(state)
        uiHelpers.setTextOrHide(text_title, state.title)
    }

    private fun updateFeaturedImage(state: ReaderRelatedPostUiState) {
        if (state.featuredImageUrl == null) {
            imageManager.cancelRequestAndClearImageView(image_featured)
        } else {
            imageManager.loadImageWithCorners(
                    image_featured,
                    PHOTO,
                    state.featuredImageUrl,
                    uiHelpers.getPxOfUiDimen(WordPress.getContext(), state.featuredImageCornerRadius)
            )
        }
    }

    private fun updateFollowButton(state: ReaderRelatedPostUiState) {
        state.followButtonUiState?.let { followButtonState ->
            follow_button.setIsFollowed(followButtonState.isFollowed)
            follow_button.isEnabled = followButtonState.isEnabled
            follow_button.setVisible(followButtonState.isVisible)
            follow_button.setOnClickListener { followButtonState.onFollowButtonClicked?.invoke() }
        } ?: follow_button.setVisible(false)
    }
}
