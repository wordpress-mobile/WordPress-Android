package org.wordpress.android.ui.reader.discover.viewholders

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.view.isVisible
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.databinding.ReaderCardviewPostNewBinding
import org.wordpress.android.datasets.ReaderThumbnailTable
import org.wordpress.android.ui.reader.adapters.ReaderMenuAdapter
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils.VideoThumbnailUrlListener
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.extensions.expandTouchTargetArea
import org.wordpress.android.util.extensions.viewBinding
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR_CIRCULAR
import org.wordpress.android.util.image.ImageType.PHOTO_ROUNDED_CORNERS
import org.wordpress.android.util.image.ImageType.VIDEO

class ReaderPostNewViewHolder(
    private val uiHelpers: UiHelpers,
    private val imageManager: ImageManager,
    private val readerTracker: ReaderTracker,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    parentView: ViewGroup
) : ReaderViewHolder<ReaderCardviewPostNewBinding>(parentView.viewBinding(ReaderCardviewPostNewBinding::inflate)) {
    init {
        with(binding) {
            moreMenu.expandTouchTargetArea(R.dimen.reader_more_image_extra_padding, false)
        }
    }

    override fun onBind(uiState: ReaderCardUiState) = with(binding) {
        val state = uiState as ReaderPostUiState

        // Blog section
        updateBlogSection(state)

        // More menu
        uiHelpers.updateVisibility(moreMenu, state.moreMenuVisibility)
        moreMenu.setOnClickListener { uiState.onMoreButtonClicked.invoke(state) }

        // Featured image section
        updateFeaturedImage(state)
        uiHelpers.updateVisibility(imageVideoOverlay, state.videoOverlayVisibility)
        uiHelpers.updateVisibility(thumbnailStrip, state.thumbnailStripSection != null)
        state.thumbnailStripSection?.let {
            thumbnailStrip.loadThumbnails(it.images, it.isPrivate, it.content)
        }
        loadVideoThumbnail(state)
        imageVideoOverlay.setOnClickListener { state.onVideoOverlayClicked(uiState.postId, uiState.blogId) }

        // Content section
        uiHelpers.setTextOrHide(textTitle, state.title)
        uiHelpers.setTextOrHide(textExcerpt, state.excerpt)
        postContainer.setOnClickListener {
            readerTracker.trackBlog(
                AnalyticsTracker.Stat.READER_POST_CARD_TAPPED,
                state.blogId,
                state.feedId,
                state.isFollowed,
                state.source
            )
            state.onItemClicked(uiState.postId, uiState.blogId)
        }

        // Interaction counts section
        updateInteractionCountsSection(state)

        // Action buttons section
        updateActionButton(uiState.postId, uiState.blogId, uiState.likeAction, like)
        updateActionButton(uiState.postId, uiState.blogId, uiState.reblogAction, reblog)
        updateActionButton(uiState.postId, uiState.blogId, uiState.commentsAction, comment)

        state.moreMenuItems?.let {
            renderMoreMenu(state, state.moreMenuItems, moreMenu)
        }

        state.onItemRendered.invoke(uiState)
    }

    private fun updateInteractionCountsSection(state: ReaderPostUiState) = with(binding) {
        val likeCount = state.interactionSection.likeCount
        val commentCount = state.interactionSection.commentCount

        val likeLabel = ReaderUtils.getShortLikeLabelText(viewContext, likeCount).takeIf { likeCount > 0 }
        val commentLabel = ReaderUtils.getShortCommentLabelText(viewContext, commentCount).takeIf { commentCount > 0 }

        uiHelpers.setTextOrHide(readerCardLikeCount, likeLabel)
        uiHelpers.setTextOrHide(readerCardCommentCount, commentLabel)
        readerCardDotSeparator.isVisible = likeLabel != null && commentLabel != null
    }

    private fun updateBlogSection(state: ReaderPostUiState) = with(binding.layoutBlogSection) {
        updateAvatarOrBlavatar(state)
        uiHelpers.setTextOrHide(blogSectionTextBlogName, state.blogSection.blogName)
        uiHelpers.setTextOrHide(blogSectionTextDateline, state.blogSection.dateLine)

        state.blogSection.onClicked?.let { onClicked ->
            root.setOnClickListener {
                onClicked.invoke(state.postId, state.blogId)
            }
        } ?: run {
            root.setOnClickListener(null)
            root.isClickable = false
        }
    }

    private fun updateAvatarOrBlavatar(state: ReaderPostUiState) = with(binding.layoutBlogSection) {
        var isShowingAnyAvatar = false

        uiHelpers.updateVisibility(blogSectionImageBlogAvatar, state.blogSection.avatarOrBlavatarUrl != null)
        if (state.blogSection.avatarOrBlavatarUrl == null) {
            imageManager.cancelRequestAndClearImageView(blogSectionImageBlogAvatar)
        } else {
            isShowingAnyAvatar = true
            imageManager.loadIntoCircle(
                blogSectionImageBlogAvatar,
                state.blogSection.blavatarType, state.blogSection.avatarOrBlavatarUrl
            )
        }

        val canShowAuthorsAvatar = state.blogSection.authorAvatarUrl != null && state.blogSection.isAuthorAvatarVisible
        uiHelpers.updateVisibility(blogSectionImageAuthorAvatar, canShowAuthorsAvatar)

        if (!canShowAuthorsAvatar) {
            imageManager.cancelRequestAndClearImageView(blogSectionImageAuthorAvatar)
        } else {
            isShowingAnyAvatar = true
            imageManager.loadIntoCircle(
                blogSectionImageAuthorAvatar,
                BLAVATAR_CIRCULAR,
                state.blogSection.authorAvatarUrl!!
            )
        }

        blogSectionAvatarContainer.isVisible = isShowingAnyAvatar
    }

    private fun updateFeaturedImage(state: ReaderPostUiState) = with(binding) {
        uiHelpers.updateVisibility(imageFeatured, state.featuredImageVisibility)
        if (state.featuredImageUrl == null) {
            imageManager.cancelRequestAndClearImageView(imageFeatured)
        } else {
            imageManager.loadImageWithCorners(
                imageFeatured,
                PHOTO_ROUNDED_CORNERS,
                state.featuredImageUrl,
                uiHelpers.getPxOfUiDimen(WordPress.getContext(), state.featuredImageCornerRadius)
            )
        }
    }

    private fun updateActionButton(postId: Long, blogId: Long, state: PrimaryAction, view: View) {
        view.isVisible = state.isEnabled
        view.isSelected = state.isSelected
        view.contentDescription = state.contentDescription?.let { uiHelpers.getTextOfUiString(view.context, it) }
        view.setOnClickListener {
            // If it's a like action, we want to update the UI right away. If there's an error, we'll revert
            // the UI change.
            if (state.type == ReaderPostCardActionType.LIKE && networkUtilsWrapper.isNetworkAvailable()) {
                view.isSelected = !view.isSelected
            }
            state.onClicked?.invoke(postId, blogId, state.type)
        }
    }

    private fun loadVideoThumbnail(state: ReaderPostUiState) = with(binding) {
        /* TODO ideally, we'd be passing just a thumbnail url in the UiState. However, the code for retrieving
            thumbnail from full video URL needs to be fully refactored. */
        state.fullVideoUrl?.let { videoUrl ->
            ReaderVideoUtils.retrieveVideoThumbnailUrl(videoUrl, object : VideoThumbnailUrlListener {
                override fun showThumbnail(thumbnailUrl: String) {
                    imageManager.loadImageWithCorners(
                        imageFeatured,
                        PHOTO_ROUNDED_CORNERS,
                        thumbnailUrl,
                        uiHelpers.getPxOfUiDimen(WordPress.getContext(), state.featuredImageCornerRadius)
                    )
                }

                override fun showPlaceholder() {
                    imageManager.load(imageFeatured, VIDEO)
                }

                override fun cacheThumbnailUrl(thumbnailUrl: String) {
                    ReaderThumbnailTable.addThumbnail(state.postId, videoUrl, thumbnailUrl)
                }
            })
        }
    }

    private fun renderMoreMenu(uiState: ReaderPostUiState, actions: List<ReaderPostCardAction>, v: View) {
        readerTracker.track(AnalyticsTracker.Stat.POST_CARD_MORE_TAPPED)
        val listPopup = ListPopupWindow(v.context)
        listPopup.width = v.context.resources.getDimensionPixelSize(R.dimen.menu_item_width)
        listPopup.setAdapter(ReaderMenuAdapter(v.context, uiHelpers, actions))
        listPopup.setDropDownGravity(Gravity.END)
        listPopup.anchorView = v
        listPopup.isModal = true
        listPopup.setOnItemClickListener { _, _, position, _ ->
            listPopup.dismiss()
            val item = actions[position]
            item.onClicked?.invoke(uiState.postId, uiState.blogId, item.type)
        }
        listPopup.setOnDismissListener { uiState.onMoreDismissed.invoke(uiState) }
        listPopup.show()
    }
}
