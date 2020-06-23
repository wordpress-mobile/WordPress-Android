package org.wordpress.android.ui.reader.discover.viewholders

import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.reader_cardview_post.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.ReaderCardUiState.ReaderPostUiState.ActionUiState
import org.wordpress.android.ui.reader.views.ReaderIconCountView
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR_CIRCULAR
import org.wordpress.android.util.image.ImageType.READER

class ReaderPostViewHolder(
    private val uiHelpers: UiHelpers,
    private val imageManager: ImageManager,
    parentView: ViewGroup
) : ReaderViewHolder(parentView, R.layout.reader_cardview_post) {
    override fun onBind(uiState: ReaderCardUiState) {
        val state = uiState as ReaderPostUiState
        // TODO malinjir handle mRootLayoutConstraintSet - see ReaderPostAdapter line 450

        // Header section
        initAvatarOrBlavatar(state)
        uiHelpers.setTextOrHide(text_author_and_blog_name, state.blogName)
        uiHelpers.setTextOrHide(text_blog_url, state.blogUrl)
        uiHelpers.updateVisibility(dot_separator, state.dotSeparatorVisibility)
        uiHelpers.setTextOrHide(text_dateline, state.dateLine)
        uiHelpers.updateVisibility(image_more, state.moreMenuVisibility)

        // Featured image section
        initFeaturedImage(state)
        uiHelpers.updateVisibility(image_video_overlay, state.videoOverlayVisibility)
        uiHelpers.setTextOrHide(text_photo_title, state.photoTitle)
        uiHelpers.updateVisibility(frame_photo, state.photoFrameVisibility)
        // TODO malinjir thumbnail gallery strip
        // TODO malinjir video thumbnail

        // Content section
        uiHelpers.setTextOrHide(text_title, state.title)
        uiHelpers.setTextOrHide(text_excerpt, state.excerpt)
        post_container.setOnClickListener { state.onItemClicked(uiState.postId, uiState.blogId) }

        // Discover section
        initDiscoverSection(state)

        // Action buttons section
        initActionButton(uiState.postId, uiState.blogId, uiState.likeAction, count_likes)
        initActionButton(uiState.postId, uiState.blogId, uiState.reblogAction, reblog)
        initActionButton(uiState.postId, uiState.blogId, uiState.commentsAction, count_comments)
        initActionButton(uiState.postId, uiState.blogId, uiState.bookmarkAction, bookmark)

        state.onItemRendered.invoke(uiState.postId, uiState.blogId)
    }

    private fun initFeaturedImage(state: ReaderPostUiState) {
        uiHelpers.updateVisibility(image_featured, state.featuredImageUrl != null)
        if (state.featuredImageUrl == null) {
            imageManager.cancelRequestAndClearImageView(image_featured)
        } else {
            imageManager.loadImageWithCorners(
                    image_featured,
                    READER,
                    state.featuredImageUrl,
                    uiHelpers.getPxOfUiDimen(WordPress.getContext(), state.featuredImageCornerRadius)
            )
        }
    }

    private fun initAvatarOrBlavatar(state: ReaderPostUiState) {
        uiHelpers.updateVisibility(image_avatar_or_blavatar, state.avatarOrBlavatarUrl != null)
        if (state.avatarOrBlavatarUrl == null) {
            imageManager.cancelRequestAndClearImageView(image_avatar_or_blavatar)
        } else {
            imageManager.loadIntoCircle(
                    image_avatar_or_blavatar,
                    BLAVATAR_CIRCULAR, state.avatarOrBlavatarUrl
            )
        }
    }

    private fun initDiscoverSection(state: ReaderPostUiState) {
        uiHelpers.updateVisibility(layout_discover, state.discoverSection != null)
        uiHelpers.setTextOrHide(text_discover, state.discoverSection?.discoverText)
        if (state.discoverSection?.discoverAvatarUrl == null) {
            imageManager.cancelRequestAndClearImageView(image_discover_avatar)
        } else {
            // TODO do we need to use `imagemanager.load` for blavatar?
            imageManager.loadIntoCircle(
                    image_discover_avatar,
                    state.discoverSection.imageType,
                    state.discoverSection.discoverAvatarUrl
            )
        }
        // TODO malinjir handle on discover click
    }

    private fun initActionButton(postId: Long, blogId: Long, state: ActionUiState, view: View) {
        if (view is ReaderIconCountView) {
            view.setCount(state.count)
        }
        view.isEnabled = state.isEnabled
        view.isSelected = state.isSelected
        view.contentDescription = state.contentDescription?.let { uiHelpers.getTextOfUiString(view.context, it) }
        view.setOnClickListener { state.onClicked?.invoke(postId, blogId, state.isSelected) }
    }
}
