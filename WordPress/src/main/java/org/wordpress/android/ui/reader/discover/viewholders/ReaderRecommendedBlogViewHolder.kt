package org.wordpress.android.ui.reader.discover.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.databinding.ReaderRecommendedBlogItemBinding
import org.wordpress.android.databinding.ReaderRecommendedBlogItemNewBinding
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState.ReaderRecommendedBlogUiState
import org.wordpress.android.ui.reader.discover.viewholders.ReaderRecommendedBlogViewHolder.ReaderRecommendedBlogBinding.ImprovementsDisabled
import org.wordpress.android.ui.reader.discover.viewholders.ReaderRecommendedBlogViewHolder.ReaderRecommendedBlogBinding.ImprovementsEnabled
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
            ImprovementsEnabled(
                root = root,
                siteName = siteName,
                siteUrl = siteUrl,
                siteDescription = siteDescription,
                siteIcon = siteIcon,
            )
        }
    } else {
        with(parent.viewBinding(ReaderRecommendedBlogItemBinding::inflate)) {
            ImprovementsDisabled(
                root = root,
                siteName = siteName,
                siteUrl = siteUrl,
                siteDescription = siteDescription,
                siteFollowIcon = siteFollowIcon,
                siteIcon = siteIcon,
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
        if (binding is ImprovementsDisabled) {
            with(binding.siteFollowIcon) {
                if (!isReaderImprovementsEnabled) {
                    isVisible = true
                    setIsFollowed(uiState.isFollowed)
                    contentDescription = context.getString(uiState.followContentDescription.stringRes)
                    setOnClickListener {
                        uiState.onFollowClicked(uiState)
                    }
                } else {
                    isVisible = false
                }
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

    abstract class ReaderRecommendedBlogBinding {
        abstract val root: View
        abstract val siteName: TextView
        abstract val siteUrl: TextView
        abstract val siteDescription: TextView
        abstract val siteIcon: ImageView

        data class ImprovementsDisabled(
            override val root: View,
            override val siteName: TextView,
            override val siteUrl: TextView,
            override val siteDescription: TextView,
            override val siteIcon: ImageView,
            val siteFollowIcon: ReaderFollowButton
        ) : ReaderRecommendedBlogBinding()

        data class ImprovementsEnabled(
            override val root: View,
            override val siteName: TextView,
            override val siteUrl: TextView,
            override val siteDescription: TextView,
            override val siteIcon: ImageView,
        ) : ReaderRecommendedBlogBinding()
    }
}
