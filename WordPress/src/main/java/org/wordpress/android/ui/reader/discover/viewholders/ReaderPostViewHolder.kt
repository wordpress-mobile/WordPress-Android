package org.wordpress.android.ui.reader.discover.viewholders

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.ListPopupWindow
import kotlinx.android.synthetic.main.reader_cardview_post.*
import org.wordpress.android.R
import org.wordpress.android.R.dimen
import org.wordpress.android.WordPress
import org.wordpress.android.datasets.ReaderThumbnailTable
import org.wordpress.android.ui.reader.adapters.ReaderMenuAdapter
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.SecondaryAction
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils
import org.wordpress.android.ui.reader.utils.ReaderVideoUtils.VideoThumbnailUrlListener
import org.wordpress.android.ui.reader.views.ReaderIconCountView
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.expandTouchTargetArea
import org.wordpress.android.util.getDrawableResIdFromAttribute
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR_CIRCULAR
import org.wordpress.android.util.image.ImageType.PHOTO_ROUNDED_CORNERS
import org.wordpress.android.util.image.ImageType.VIDEO

class ReaderPostViewHolder(
    private val uiHelpers: UiHelpers,
    private val imageManager: ImageManager,
    parentView: ViewGroup
) : ReaderViewHolder(parentView, R.layout.reader_cardview_post) {
    val viewContext: Context = post_container.context

    init {
        layout_discover.expandTouchTargetArea(R.dimen.reader_discover_layout_extra_padding, true)
        image_more.expandTouchTargetArea(R.dimen.reader_more_image_extra_padding, false)
    }

    override fun onBind(uiState: ReaderCardUiState) {
        val state = uiState as ReaderPostUiState
        // TODO malinjir animate like button on click

        // Header section
        updateAvatarOrBlavatar(state)
        uiHelpers.setTextOrHide(text_author_and_blog_name, state.blogName)
        uiHelpers.setTextOrHide(text_blog_url, state.blogUrl)
        uiHelpers.updateVisibility(dot_separator, state.dotSeparatorVisibility)
        uiHelpers.setTextOrHide(text_dateline, state.dateLine)
        uiHelpers.updateVisibility(image_more, state.moreMenuVisibility)
        image_more.setOnClickListener { onMoreClicked(uiState, uiState.moreMenuItems, it) }
        layout_post_header.setBackgroundResource(
                layout_post_header.context.getDrawableResIdFromAttribute(uiState.postHeaderClickData?.background ?: 0)
        )
        uiState.postHeaderClickData?.onPostHeaderViewClicked?.let {
            layout_post_header.setOnClickListener {
                uiState.postHeaderClickData.onPostHeaderViewClicked.invoke(uiState.postId, uiState.blogId)
            }
        } ?: run {
            layout_post_header.setOnClickListener(null)
            layout_post_header.isClickable = false
        }

        // Featured image section
        updateFeaturedImage(state)
        uiHelpers.updateVisibility(image_video_overlay, state.videoOverlayVisibility)
        uiHelpers.setTextOrHide(text_photo_title, state.photoTitle)
        uiHelpers.updateVisibility(frame_photo, state.photoFrameVisibility)
        uiHelpers.updateVisibility(thumbnail_strip, state.thumbnailStripSection != null)
        state.thumbnailStripSection?.let {
            thumbnail_strip.loadThumbnails(it.images, it.isPrivate, it.content)
        }
        loadVideoThumbnail(state)

        // Content section
        uiHelpers.setTextOrHide(text_title, state.title)
        uiHelpers.setTextOrHide(text_excerpt, state.excerpt)
        post_container.setOnClickListener { state.onItemClicked(uiState.postId, uiState.blogId) }

        // Discover section
        updateDiscoverSection(state)

        // Action buttons section
        updateActionButton(uiState.postId, uiState.blogId, uiState.likeAction, count_likes)
        updateActionButton(uiState.postId, uiState.blogId, uiState.reblogAction, reblog)
        updateActionButton(uiState.postId, uiState.blogId, uiState.commentsAction, count_comments)
        updateActionButton(uiState.postId, uiState.blogId, uiState.bookmarkAction, bookmark)

        state.onItemRendered.invoke(uiState.postId, uiState.blogId)
    }

    private fun updateFeaturedImage(state: ReaderPostUiState) {
        uiHelpers.updateVisibility(image_featured, state.featuredImageUrl != null)
        if (state.featuredImageUrl == null) {
            imageManager.cancelRequestAndClearImageView(image_featured)
        } else {
            imageManager.loadImageWithCorners(
                    image_featured,
                    PHOTO_ROUNDED_CORNERS,
                    state.featuredImageUrl,
                    uiHelpers.getPxOfUiDimen(WordPress.getContext(), state.featuredImageCornerRadius)
            )
        }
    }

    private fun updateAvatarOrBlavatar(state: ReaderPostUiState) {
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

    private fun updateDiscoverSection(state: ReaderPostUiState) {
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
        layout_discover.setOnClickListener {
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

    private fun loadVideoThumbnail(state: ReaderPostUiState) {
        /* TODO ideally, we'd be passing just a thumbnail url in the UiState. However, the code for retrieving
            thumbnail from full video URL needs to be fully refactored. */
        state.fullVideoUrl?.let { videoUrl ->
            ReaderVideoUtils.retrieveVideoThumbnailUrl(videoUrl, object : VideoThumbnailUrlListener {
                override fun showThumbnail(thumbnailUrl: String) {
                    imageManager.loadImageWithCorners(
                            image_featured,
                            PHOTO_ROUNDED_CORNERS,
                            thumbnailUrl,
                            uiHelpers.getPxOfUiDimen(WordPress.getContext(), state.featuredImageCornerRadius)
                    )
                }

                override fun showPlaceholder() {
                    imageManager.load(image_featured, VIDEO)
                }

                override fun cacheThumbnailUrl(thumbnailUrl: String) {
                    ReaderThumbnailTable.addThumbnail(state.postId, videoUrl, thumbnailUrl)
                }
            })
        }
    }

    private fun onMoreClicked(uiState: ReaderPostUiState, actions: List<SecondaryAction>, v: View) {
        // TODO malinjir the popup menu was reused from the legacy implementation. It needs to be refactored.
        val listPopup = ListPopupWindow(v.context)
        listPopup.width = v.context.resources.getDimensionPixelSize(dimen.menu_item_width)
        listPopup.setAdapter(ReaderMenuAdapter(v.context, uiHelpers, actions))
        listPopup.setDropDownGravity(Gravity.END)
        listPopup.anchorView = v
        listPopup.isModal = true
        listPopup.setOnItemClickListener { _, _, position, _ ->
            listPopup.dismiss()
            val item = actions[position]
            item.onClicked.invoke(uiState.postId, uiState.blogId, item.type)
        }
        listPopup.show()
    }
}
