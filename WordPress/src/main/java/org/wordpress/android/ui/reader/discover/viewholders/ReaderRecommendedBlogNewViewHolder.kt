package org.wordpress.android.ui.reader.discover.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.databinding.ReaderRecommendedBlogItemNewBinding
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState.ReaderRecommendedBlogUiState
import org.wordpress.android.ui.reader.views.ReaderFollowButton
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR_CIRCULAR

class ReaderRecommendedBlogNewViewHolder(
    parent: ViewGroup,
    private val imageManager: ImageManager,
    private val binding: ReaderRecommendedBlogBinding =
        with(parent.viewBinding(ReaderRecommendedBlogItemNewBinding::inflate)) {
            ReaderRecommendedBlogBinding(
                root = root,
                siteName = siteName,
                siteUrl = siteUrl,
                siteIcon = siteIcon,
                siteFollowIcon = siteFollowIcon,
            )
        },
) : RecyclerView.ViewHolder(binding.root) {
    fun onBind(uiState: ReaderRecommendedBlogUiState) =
        with(binding) {
            siteName.text = uiState.name
            siteUrl.text = uiState.url
            updateSiteFollowIcon(uiState, this)
            updateBlogImage(uiState.iconUrl)
            root.setOnClickListener {
                uiState.onItemClicked(uiState.blogId, uiState.feedId, uiState.isFollowed)
            }
        }

    private fun updateSiteFollowIcon(uiState: ReaderRecommendedBlogUiState, binding: ReaderRecommendedBlogBinding) {
        with(binding.siteFollowIcon) {
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

    data class ReaderRecommendedBlogBinding(
        val root: View,
        val siteName: TextView,
        val siteUrl: TextView,
        val siteIcon: ImageView,
        val siteFollowIcon: ReaderFollowButton,
    )
}
