package org.wordpress.android.ui.reader.discover.viewholders

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.ListPopupWindow
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.databinding.ReaderCardviewPostBinding
import org.wordpress.android.datasets.ReaderThumbnailTable
import org.wordpress.android.ui.reader.adapters.ReaderMenuAdapter
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils.VideoThumbnailUrlListener
import org.wordpress.android.ui.reader.views.ReaderIconCountView
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.expandTouchTargetArea
import org.wordpress.android.util.extensions.getDrawableResIdFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR_CIRCULAR
import org.wordpress.android.util.image.ImageType.PHOTO_ROUNDED_CORNERS
import org.wordpress.android.util.image.ImageType.VIDEO
import org.wordpress.android.util.extensions.viewBinding

class ReaderPostViewHolder(
    private val uiHelpers: UiHelpers,
    private val imageManager: ImageManager,
    private val readerTracker: ReaderTracker,
    parentView: ViewGroup
) : ReaderViewHolder<ReaderCardviewPostBinding>(parentView.viewBinding(ReaderCardviewPostBinding::inflate)) {
    val viewContext: Context = binding.postContainer.context

    init {
        with(binding) {
            layoutDiscover.expandTouchTargetArea(R.dimen.reader_discover_layout_extra_padding, true)
            imageMore.expandTouchTargetArea(R.dimen.reader_more_image_extra_padding, false)
        }
    }

    override fun onBind(uiState: ReaderCardUiState) = with(binding) {
        val state = uiState as ReaderPostUiState

        // Expandable tags section
        uiHelpers.updateVisibility(expandableTagsView, state.expandableTagsViewVisibility)
        expandableTagsView.updateUi(state.tagItems)

        // Blog section
        updateBlogSection(state)

        // More menu
        uiHelpers.updateVisibility(imageMore, state.moreMenuVisibility)
        imageMore.setOnClickListener { uiState.onMoreButtonClicked.invoke(state) }

        // Featured image section
        updateFeaturedImage(state)
        uiHelpers.updateVisibility(imageVideoOverlay, state.videoOverlayVisibility)
        uiHelpers.setTextOrHide(textPhotoTitle, state.photoTitle)
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

        // Discover section
        updateDiscoverSection(state)

        // Action buttons section
        updateActionButton(uiState.postId, uiState.blogId, uiState.likeAction, countLikes)
        updateActionButton(uiState.postId, uiState.blogId, uiState.reblogAction, reblog)
        updateActionButton(uiState.postId, uiState.blogId, uiState.commentsAction, countComments)
        updateActionButton(uiState.postId, uiState.blogId, uiState.bookmarkAction, bookmark)

        state.moreMenuItems?.let {
            renderMoreMenu(state, state.moreMenuItems, imageMore)
        }

        state.onItemRendered.invoke(uiState)
    }

    private fun updateBlogSection(state: ReaderPostUiState) = with(binding.layoutBlogSection) {
        updateAvatarOrBlavatar(state)
        uiHelpers.setTextOrHide(textAuthorAndBlogName, state.blogSection.blogName)
        uiHelpers.setTextOrHide(textBlogUrl, state.blogSection.blogUrl)
        uiHelpers.updateVisibility(dotSeparator, state.blogSection.dotSeparatorVisibility)
        uiHelpers.setTextOrHide(textDateline, state.blogSection.dateLine)

        root.setBackgroundResource(
                root.context.getDrawableResIdFromAttribute(
                        state.blogSection.blogSectionClickData?.background ?: 0
                )
        )
        state.blogSection.blogSectionClickData?.onBlogSectionClicked?.let {
            root.setOnClickListener {
                state.blogSection.blogSectionClickData.onBlogSectionClicked.invoke(state.postId, state.blogId)
            }
        } ?: run {
            root.setOnClickListener(null)
            root.isClickable = false
        }
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

    private fun updateAvatarOrBlavatar(state: ReaderPostUiState) = with(binding.layoutBlogSection) {
        uiHelpers.updateVisibility(imageAvatarOrBlavatar, state.blogSection.avatarOrBlavatarUrl != null)
        if (state.blogSection.avatarOrBlavatarUrl == null) {
            imageManager.cancelRequestAndClearImageView(imageAvatarOrBlavatar)
        } else {
            imageManager.loadIntoCircle(
                    imageAvatarOrBlavatar,
                    state.blogSection.blavatarType, state.blogSection.avatarOrBlavatarUrl
            )
        }

        val canShowAuthorsAvatar = state.blogSection.authorAvatarUrl != null && state.blogSection.isAuthorAvatarVisible
        uiHelpers.updateVisibility(authorsAvatar, canShowAuthorsAvatar)

        if (!canShowAuthorsAvatar) {
            imageManager.cancelRequestAndClearImageView(authorsAvatar)
        } else {
            imageManager.loadIntoCircle(authorsAvatar, BLAVATAR_CIRCULAR, state.blogSection.authorAvatarUrl!!)
        }
    }

    private fun updateDiscoverSection(state: ReaderPostUiState) = with(binding) {
        uiHelpers.updateVisibility(imageDiscoverAvatar, state.discoverSection?.discoverAvatarUrl != null)
        uiHelpers.updateVisibility(layoutDiscover, state.discoverSection != null)
        uiHelpers.setTextOrHide(textDiscover, state.discoverSection?.discoverText)
        if (state.discoverSection?.discoverAvatarUrl == null) {
            imageManager.cancelRequestAndClearImageView(imageDiscoverAvatar)
        } else {
            // TODO do we need to use `imagemanager.load` for blavatar?
            imageManager.loadIntoCircle(
                    imageDiscoverAvatar,
                    state.discoverSection.imageType,
                    state.discoverSection.discoverAvatarUrl
            )
        }
        layoutDiscover.setOnClickListener {
            state.discoverSection?.onDiscoverClicked?.invoke(state.postId, state.blogId)
        }
    }

    private fun updateActionButton(postId: Long, blogId: Long, state: PrimaryAction, view: View) {
        if (view is ReaderIconCountView) {
            view.setCount(state.count)
        }
        view.isEnabled = state.isEnabled
        view.isSelected = state.isSelected
        view.contentDescription = state.contentDescription?.let { uiHelpers.getTextOfUiString(view.context, it) }
        view.setOnClickListener { state.onClicked?.invoke(postId, blogId, state.type) }
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
