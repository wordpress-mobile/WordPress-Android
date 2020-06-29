package org.wordpress.android.ui.reader.discover

import android.view.View
import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderCardType.DEFAULT
import org.wordpress.android.models.ReaderCardType.GALLERY
import org.wordpress.android.models.ReaderCardType.PHOTO
import org.wordpress.android.models.ReaderCardType.VIDEO
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostDiscoverData
import org.wordpress.android.models.ReaderPostDiscoverData.DiscoverType.EDITOR_PICK
import org.wordpress.android.models.ReaderPostDiscoverData.DiscoverType.OTHER
import org.wordpress.android.models.ReaderPostDiscoverData.DiscoverType.SITE_PICK
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState.DiscoverLayoutUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState.GalleryThumbnailStripData
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState.PostHeaderClickData
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BOOKMARK
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.LIKE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REBLOG
import org.wordpress.android.ui.reader.utils.ReaderImageScannerProvider
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.utils.UiDimen.UIDimenRes
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.GravatarUtilsWrapper
import org.wordpress.android.util.UrlUtilsWrapper
import org.wordpress.android.util.image.ImageType.AVATAR
import org.wordpress.android.util.image.ImageType.BLAVATAR
import javax.inject.Inject

@Reusable
class ReaderPostUiStateBuilder @Inject constructor(
    private val accountStore: AccountStore,
    private val urlUtilsWrapper: UrlUtilsWrapper,
    private val gravatarUtilsWrapper: GravatarUtilsWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val readerImageScannerProvider: ReaderImageScannerProvider,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val readerPostMoreButtonUiStateBuilder: ReaderPostMoreButtonUiStateBuilder
) {
    // TODO malinjir move this to a bg thread
    fun mapPostToUiState(
        post: ReaderPost,
        photonWidth: Int,
        photonHeight: Int,
            // TODO malinjir try to refactor/remove this parameter
        postListType: ReaderPostListType,
            // TODO malinjir try to refactor/remove this parameter
        isBookmarkList: Boolean,
        onButtonClicked: (Long, Long, Boolean, ReaderPostCardActionType) -> Unit,
        onItemClicked: (Long, Long) -> Unit,
        onItemRendered: (Long, Long) -> Unit,
        onDiscoverSectionClicked: (Long, Long) -> Unit,
        onMoreButtonClicked: (Long, Long, View) -> Unit,
        onVideoOverlayClicked: (Long, Long) -> Unit,
        onPostHeaderViewClicked: (Long, Long) -> Unit
    ): ReaderPostUiState {
        return ReaderPostUiState(
                postId = post.postId,
                blogId = post.blogId,
                blogUrl = buildBlogUrl(post),
                dateLine = buildDateLine(post),
                avatarOrBlavatarUrl = buildAvatarOrBlavatarUrl(post),
                blogName = buildBlogName(post),
                excerpt = buildExcerpt(post),
                title = buildTitle(post),
                photoFrameVisibility = buildPhotoFrameVisibility(post),
                photoTitle = buildPhotoTitle(post),
                featuredImageUrl = buildFeaturedImageUrl(post, photonWidth, photonHeight),
                featuredImageCornerRadius = UIDimenRes(R.dimen.reader_featured_image_corner_radius),
                thumbnailStripSection = buildThumbnailStripUrls(post),
                videoOverlayVisibility = buildVideoOverlayVisibility(post),
                moreMenuVisibility = accountStore.hasAccessToken() && postListType == ReaderPostListType.TAG_FOLLOWED,
                fullVideoUrl = buildFullVideoUrl(post),
                discoverSection = buildDiscoverSection(post, onDiscoverSectionClicked),
                bookmarkAction = buildBookmarkSection(post, onButtonClicked),
                likeAction = buildLikeSection(post, isBookmarkList, onButtonClicked),
                reblogAction = buildReblogSection(post, onButtonClicked),
                commentsAction = buildCommentsSection(post, isBookmarkList, onButtonClicked),
                onItemClicked = onItemClicked,
                onItemRendered = onItemRendered,
                onMoreButtonClicked = onMoreButtonClicked,
                onVideoOverlayClicked = onVideoOverlayClicked,
                postHeaderClickData = buildOnPostHeaderViewClicked(onPostHeaderViewClicked, postListType),
                moreMenuItems = readerPostMoreButtonUiStateBuilder.buildMoreMenuItems(
                        post,
                        postListType,
                        onButtonClicked
                )
        )
    }

    private fun buildOnPostHeaderViewClicked(
        onPostHeaderViewClicked: (Long, Long) -> Unit,
        postListType: ReaderPostListType
    ): PostHeaderClickData? {
        return if (postListType != ReaderPostListType.BLOG_PREVIEW) {
            PostHeaderClickData(onPostHeaderViewClicked, android.R.attr.selectableItemBackground)
        } else {
            null
        }
    }

    private fun buildBlogUrl(post: ReaderPost) = post
            .takeIf { it.hasBlogUrl() }
            ?.blogUrl
            ?.let { urlUtilsWrapper.removeScheme(it) }

    private fun buildDiscoverSection(post: ReaderPost, onDiscoverSectionClicked: (Long, Long) -> Unit) =
            post.takeIf { post.isDiscoverPost && post.discoverData.discoverType != OTHER }
                    ?.let { buildDiscoverSectionUiState(post.discoverData, onDiscoverSectionClicked) }

    private fun buildFullVideoUrl(post: ReaderPost) =
            post.takeIf { post.cardType == VIDEO }
                    ?.let { post.featuredVideo }

    private fun buildVideoOverlayVisibility(post: ReaderPost) = post.cardType == VIDEO

    private fun buildThumbnailStripUrls(post: ReaderPost) =
            post.takeIf { it.cardType == GALLERY }
                    ?.let { retrieveGalleryThumbnailUrls(post) }

    private fun buildFeaturedImageUrl(post: ReaderPost, photonWidth: Int, photonHeight: Int): String? {
        return post
                .takeIf { (it.cardType == PHOTO || it.cardType == DEFAULT) && it.hasFeaturedImage() }
                ?.getFeaturedImageForDisplay(photonWidth, photonHeight)
    }

    private fun buildPhotoTitle(post: ReaderPost) =
            post.takeIf { it.cardType == PHOTO && it.hasTitle() }?.title

    private fun buildPhotoFrameVisibility(post: ReaderPost) =
            (post.hasFeaturedVideo() || post.hasFeaturedImage()) &&
                    post.cardType != GALLERY

    private fun buildTitle(post: ReaderPost) =
            post.takeIf { post.cardType != PHOTO && it.hasTitle() }?.title

    private fun buildExcerpt(post: ReaderPost) =
            post.takeIf { post.cardType != PHOTO && post.hasExcerpt() }?.excerpt

    private fun buildBlogName(post: ReaderPost) = post.takeIf { it.hasBlogName() }?.blogName

    private fun buildAvatarOrBlavatarUrl(post: ReaderPost) =
            post.takeIf { it.hasBlogImageUrl() }
                    ?.blogImageUrl
                    ?.let { gravatarUtilsWrapper.fixGravatarUrlWithResource(it, R.dimen.avatar_sz_medium) }

    private fun buildDateLine(post: ReaderPost) =
            dateTimeUtilsWrapper.javaDateToTimeSpan(post.displayDate)

    private fun buildDiscoverSectionUiState(
        discoverData: ReaderPostDiscoverData,
        onDiscoverSectionClicked: (Long, Long) -> Unit
    ): DiscoverLayoutUiState {
        // TODO malinjir don't store Spanned in VM/UiState => refactor getAttributionHtml method.
        val discoverText = discoverData.attributionHtml
        val discoverAvatarUrl = gravatarUtilsWrapper.fixGravatarUrlWithResource(
                discoverData.avatarUrl,
                R.dimen.avatar_sz_small
        )
        val discoverAvatarImageType = when (discoverData.discoverType) {
            EDITOR_PICK -> AVATAR
            SITE_PICK -> BLAVATAR
            OTHER -> throw IllegalStateException("This could should be unreachable.")
            else -> AVATAR
        }
        return DiscoverLayoutUiState(discoverText, discoverAvatarUrl, discoverAvatarImageType, onDiscoverSectionClicked)
    }

    private fun retrieveGalleryThumbnailUrls(post: ReaderPost): GalleryThumbnailStripData {
        // scan post content for images suitable in a gallery
        val images = readerImageScannerProvider.createReaderImageScanner(post.text, post.isPrivate)
                .getImageList(ReaderConstants.THUMBNAIL_STRIP_IMG_COUNT, ReaderConstants.MIN_GALLERY_IMAGE_WIDTH)
        return GalleryThumbnailStripData(images, post.isPrivate, post.text)
    }

    private fun buildBookmarkSection(
        post: ReaderPost,
        onClicked: (Long, Long, Boolean, ReaderPostCardActionType) -> Unit
    ): PrimaryAction {
        val contentDescription = if (post.isBookmarked) {
            R.string.reader_remove_bookmark
        } else {
            R.string.reader_add_bookmark
        }
        // TODO malinjir shouldn't the action be disabled just for posts which don't have blog and post id?
        return if (!post.isDiscoverPost) {
            PrimaryAction(
                    isEnabled = true,
                    isSelected = post.isBookmarked,
                    contentDescription = UiStringRes(contentDescription),
                    onClicked = onClicked,
                    type = BOOKMARK
            )
        } else {
            PrimaryAction(isEnabled = false, type = BOOKMARK)
        }
    }

    private fun buildLikeSection(
        post: ReaderPost,
        isBookmarkList: Boolean,
        onClicked: (Long, Long, Boolean, ReaderPostCardActionType) -> Unit
    ): PrimaryAction {
        val showLikes = when {
            /* TODO malinjir why we don't show likes on bookmark list??? I think we wanted
                 to keep the card as simple as possible. However, since we are showing all the actions now, some of them
                 are just disabled, I think it's ok to enable the action. */
            post.isDiscoverPost || isBookmarkList -> false
            !accountStore.hasAccessToken() -> post.numLikes > 0
            else -> post.canLikePost()
        }

        return if (showLikes) {
            PrimaryAction(
                    isEnabled = true,
                    isSelected = post.isLikedByCurrentUser,
                    contentDescription = UiStringText(
                            readerUtilsWrapper.getLongLikeLabelText(post.numLikes, post.isLikedByCurrentUser)
                    ),
                    count = post.numLikes,
                    onClicked = if (accountStore.hasAccessToken()) onClicked else null,
                    type = LIKE
            )
        } else {
            PrimaryAction(isEnabled = false, type = LIKE)
        }
    }

    private fun buildReblogSection(
        post: ReaderPost,
        onReblogClicked: (Long, Long, Boolean, ReaderPostCardActionType) -> Unit
    ): PrimaryAction {
        val canReblog = !post.isPrivate && accountStore.hasAccessToken()
        return if (canReblog) {
            // TODO Add content description
            PrimaryAction(isEnabled = true, onClicked = onReblogClicked, type = REBLOG)
        } else {
            PrimaryAction(isEnabled = false, type = REBLOG)
        }
    }

    private fun buildCommentsSection(
        post: ReaderPost,
        isBookmarkList: Boolean,
        onCommentsClicked: (Long, Long, Boolean, ReaderPostCardActionType) -> Unit
    ): PrimaryAction {
        val showComments = when {
            /* TODO malinjir why we don't show comments on bookmark list??? I think we wanted
                 to keep the card as simple as possible. However, since we are showing all the actions now, some of them
                 are just disabled, I think it's ok to enable the action. */
            post.isDiscoverPost || isBookmarkList -> false
            !accountStore.hasAccessToken() -> post.numLikes > 0
            else -> post.isWP && (post.isCommentsOpen || post.numReplies > 0)
        }

        // TODO Add content description
        return if (showComments) {
            PrimaryAction(
                    isEnabled = true,
                    count = post.numReplies,
                    onClicked = onCommentsClicked,
                    type = ReaderPostCardActionType.COMMENTS
            )
        } else {
            PrimaryAction(
                    isEnabled = false,
                    type = ReaderPostCardActionType.COMMENTS
            )
        }
    }
}
