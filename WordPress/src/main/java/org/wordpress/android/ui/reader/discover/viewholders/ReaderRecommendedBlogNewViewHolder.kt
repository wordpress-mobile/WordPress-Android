package org.wordpress.android.ui.reader.discover.viewholders

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.databinding.ReaderRecommendedBlogItemNewBinding
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState.ReaderRecommendedBlogUiState
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR_CIRCULAR

class ReaderRecommendedBlogNewViewHolder(
    parent: ViewGroup,
    private val imageManager: ImageManager,
    private val binding: ReaderRecommendedBlogItemNewBinding =
        parent.viewBinding(ReaderRecommendedBlogItemNewBinding::inflate)
) : RecyclerView.ViewHolder(binding.root) {
    fun onBind(uiState: ReaderRecommendedBlogUiState) =
        with(binding) {
            siteName.text = uiState.name
            siteUrl.text = uiState.url
            updateSiteFollowButton(uiState, this)
            updateBlogImage(uiState.iconUrl)
            root.setOnClickListener {
                uiState.onItemClicked(uiState.blogId, uiState.feedId, uiState.isFollowed)
            }
        }

    private fun updateSiteFollowButton(
        uiState: ReaderRecommendedBlogUiState,
        binding: ReaderRecommendedBlogItemNewBinding
    ) {
        with(binding.siteFollowButton) {
            setIsFollowed(uiState.isFollowed)
            contentDescription = context.getString(uiState.followContentDescription.stringRes)
            setOnClickListener {
                uiState.onFollowClicked(uiState)
            }
        }
    }

    private fun updateBlogImage(iconUrl: String?) = with(binding) {
        if (iconUrl != null) {
            imageManager.loadIntoCircle(
                imageView = siteIcon,
                imageType = BLAVATAR_CIRCULAR,
                imgUrl = iconUrl
            )
        } else {
            imageManager.cancelRequestAndClearImageView(siteIcon)
        }
    }
}
