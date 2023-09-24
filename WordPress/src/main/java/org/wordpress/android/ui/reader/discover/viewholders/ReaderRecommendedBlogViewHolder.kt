package org.wordpress.android.ui.reader.discover.viewholders

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.databinding.ReaderRecommendedBlogItemBinding
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState.ReaderRecommendedBlogUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR_CIRCULAR

class ReaderRecommendedBlogViewHolder(
    parent: ViewGroup,
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers,
    private val binding: ReaderRecommendedBlogItemBinding =
        parent.viewBinding(ReaderRecommendedBlogItemBinding::inflate)
) : RecyclerView.ViewHolder(binding.root) {
    fun onBind(uiState: ReaderRecommendedBlogUiState) = with(binding) {
        with(uiState) {
            siteName.text = name
            siteUrl.text = url
            uiHelpers.setTextOrHide(siteDescription, description)
            siteFollowIcon.apply {
                setIsFollowed(isFollowed)
                contentDescription = context.getString(followContentDescription.stringRes)
                setOnClickListener {
                    onFollowClicked(uiState)
                }
            }
            updateBlogImage(iconUrl)
            root.setOnClickListener {
                onItemClicked(blogId, feedId, isFollowed)
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
