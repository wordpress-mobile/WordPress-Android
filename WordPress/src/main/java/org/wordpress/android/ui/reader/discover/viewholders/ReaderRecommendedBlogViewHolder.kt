package org.wordpress.android.ui.reader.discover.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.databinding.ReaderRecommendedBlogItemBinding
import org.wordpress.android.databinding.ReaderRecommendedBlogItemNewBinding
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState.ReaderRecommendedBlogUiState
import org.wordpress.android.ui.reader.views.ReaderFollowButton
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR_CIRCULAR

class ReaderRecommendedBlogViewHolder(
    parent: ViewGroup,
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers,
    private val isReaderImprovementsEnabled: Boolean,
    private val binding: ReaderRecommendedBlogBinding = if(isReaderImprovementsEnabled) {
        with(parent.viewBinding(ReaderRecommendedBlogItemNewBinding::inflate)) {
            ReaderRecommendedBlogBinding(
                root = root,
                siteName = siteName,
                siteUrl = siteUrl,
                siteDescription = siteDescription,
                siteIcon = siteIcon,
                siteFollowButton = siteFollowIcon,
            )
        }
    } else {
        with(parent.viewBinding(ReaderRecommendedBlogItemBinding::inflate)) {
            ReaderRecommendedBlogBinding(
                root = root,
                siteName = siteName,
                siteUrl = siteUrl,
                siteDescription = siteDescription,
                siteIcon = siteIcon,
                siteFollowButton = siteFollowIcon,
            )
        }
    },
) : RecyclerView.ViewHolder(binding.root) {
    fun onBind(uiState: ReaderRecommendedBlogUiState) =
        with(binding) {
            siteName.text = uiState.name
            siteUrl.text = uiState.url
            uiHelpers.setTextOrHide(siteDescription, uiState.description)
            updateSiteFollowIcon(uiState, this)
            updateBlogImage(uiState.iconUrl)
            root.setOnClickListener {
                uiState.onItemClicked(uiState.blogId, uiState.feedId, uiState.isFollowed)
            }
        }

    private fun updateSiteFollowIcon(uiState: ReaderRecommendedBlogUiState, binding: ReaderRecommendedBlogBinding) {
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

    data class ReaderRecommendedBlogBinding(
        val root: View,
        val siteName: TextView,
        val siteUrl: TextView,
        val siteDescription: TextView,
        val siteIcon: ImageView,
        val siteFollowButton: ReaderFollowButton,
    )
}
