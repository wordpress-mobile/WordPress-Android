package org.wordpress.android.ui.reader.discover.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.reader_recommended_blog_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState.ReaderRecommendedBlogUiState
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR_CIRCULAR
import org.wordpress.android.util.setVisible

class ReaderRecommendedBlogViewHolder(
    internal val parent: ViewGroup,
    private val imageManager: ImageManager,
    override val containerView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.reader_recommended_blog_item, parent, false)
) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun onBind(uiState: ReaderRecommendedBlogUiState) {
        with(uiState) {
            site_name.text = name
            site_url.text = url
            site_description.apply {
                text = description
                setVisible(isDescriptionVisible)
            }
            site_follow_icon.apply {
                setIsFollowed(isFollowed)
                contentDescription = context.getString(followContentDescription.stringRes)
                setOnClickListener {
                    onFollowClicked(blogId, feedId, !isFollowed)
                }
            }
            updateBlogImage(iconUrl)
            containerView.setOnClickListener {
                onItemClicked(blogId, feedId)
            }
        }
    }

    private fun updateBlogImage(iconUrl: String?) {
        if (iconUrl != null) {
            imageManager.loadIntoCircle(
                    imageView = site_icon,
                    imageType = BLAVATAR_CIRCULAR,
                    imgUrl = iconUrl
            )
        } else {
            imageManager.cancelRequestAndClearImageView(site_icon)
        }
    }
}
