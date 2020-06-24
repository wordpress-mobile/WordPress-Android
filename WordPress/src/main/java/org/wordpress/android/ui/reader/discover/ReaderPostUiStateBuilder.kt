package org.wordpress.android.ui.reader.discover

import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.WordPress
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
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.ReaderCardUiState.ReaderPostUiState.ActionUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.ReaderCardUiState.ReaderPostUiState.DiscoverLayoutUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.ReaderCardUiState.ReaderPostUiState.GalleryThumbnailStripData
import org.wordpress.android.ui.reader.utils.ReaderImageScannerProvider
import org.wordpress.android.ui.reader.utils.ReaderUtils
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
    private val readerImageScannerProvider: ReaderImageScannerProvider
) {
    // TODO malinjir move this to a bg thread
    fun mapPostToUiState(
        post: ReaderPost,
        photonWidth: Int,
        photonHeight: Int,
            // TODO malinjir try to refactor/remove this parameter
        isBookmarkList: Boolean,
        onBookmarkClicked: (Long, Long, Boolean) -> Unit,
        onLikeClicked: (Long, Long, Boolean) -> Unit,
        onReblogClicked: (Long, Long, Boolean) -> Unit,
        onCommentsClicked: (Long, Long, Boolean) -> Unit,
        onItemClicked: (ReaderPost) -> Unit,
        onItemRendered: (ReaderPost) -> Unit
    ): ReaderPostUiState {
        // TODO malinjir on item rendered callback -> handle load more event and trackRailcarRender

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
                thumbnailStripSection = buildThumbnailStripUrls(post),
                videoOverlayVisibility = buildVideoOverlayVisibility(post),
                // TODO malinjir Consider adding `postListType == ReaderPostListType.TAG_FOLLOWED` to showMoreMenu
                moreMenuVisibility = accountStore.hasAccessToken(),
                videoThumbnailUrl = buildVideoThumbnailUrl(post),
                discoverSection = buildDiscoverSection(post),
                bookmarkAction = buildBookmarkSection(post, onBookmarkClicked),
                likeAction = buildLikeSection(post, isBookmarkList, onLikeClicked),
                reblogAction = buildReblogSection(post, onReblogClicked),
                commentsAction = buildCommentsSection(post, isBookmarkList, onCommentsClicked),
                onItemClicked = onItemClicked,
                onItemRendered = onItemRendered
        )
    }

    private fun buildBlogUrl(post: ReaderPost) = post
            .takeIf { it.hasBlogUrl() }
            ?.blogUrl
            ?.let { urlUtilsWrapper.removeScheme(it) }

    private fun buildDiscoverSection(post: ReaderPost) =
            post.takeIf { post.isDiscoverPost && post.discoverData.discoverType != OTHER }
                    ?.let { buildDiscoverSectionUiState(post.discoverData) }

    private fun buildVideoThumbnailUrl(post: ReaderPost) =
            post.takeIf { post.cardType == VIDEO }
                    ?.let { retrieveVideoThumbnailUrl() }

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

    // TODO malinjir `post.cardType != GALLERY` might not be needed
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

    private fun buildDiscoverSectionUiState(discoverData: ReaderPostDiscoverData): DiscoverLayoutUiState {
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
        // TODO malinjir discoverLayout onClick listener.
        return DiscoverLayoutUiState(discoverText, discoverAvatarUrl, discoverAvatarImageType)
    }

    private fun retrieveVideoThumbnailUrl(): String? {
        // TODO malinjir Not yet implemented - Refactor ReaderVideoUtils.retrieveVideoThumbnailUrl
        return null
    }

    private fun retrieveGalleryThumbnailUrls(post: ReaderPost): GalleryThumbnailStripData {
        // scan post content for images suitable in a gallery
        val images = readerImageScannerProvider.createReaderImageScanner(post.text, post.isPrivate)
                .getImageList(ReaderConstants.THUMBNAIL_STRIP_IMG_COUNT, ReaderConstants.MIN_GALLERY_IMAGE_WIDTH)
        return GalleryThumbnailStripData(images, post.isPrivate)
    }

    private fun buildBookmarkSection(post: ReaderPost, onClicked: (Long, Long, Boolean) -> Unit): ActionUiState {
        val contentDescription = if (post.isBookmarked) {
            R.string.reader_remove_bookmark
        } else {
            R.string.reader_add_bookmark
        }
        // TODO malinjir shouldn't the action be disabled just for posts which don't have blog and post id?
        return if (!post.isDiscoverPost) {
            ActionUiState(
                    isEnabled = true,
                    isSelected = post.isBookmarked,
                    contentDescription = UiStringRes(contentDescription),
                    onClicked = onClicked
            )
        } else {
            ActionUiState(isEnabled = false)
        }
    }

    private fun buildLikeSection(
        post: ReaderPost,
        isBookmarkList: Boolean,
        onClicked: (Long, Long, Boolean) -> Unit
    ): ActionUiState {
        val showLikes = when {
            /* TODO malinjir why we don't show likes on bookmark list??? I think we wanted
                 to keep the card as simple as possible. However, since we are showing all the actions now, some of them
                 are just disabled, I think it's ok to enable the action. */
            post.isDiscoverPost || isBookmarkList -> false
            !accountStore.hasAccessToken() -> post.numLikes > 0
            else -> post.canLikePost()
        }

        return if (showLikes) {
            ActionUiState(
                    isEnabled = true,
                    isSelected = post.isLikedByCurrentUser,
                    // TODO malinjir remove static access and reference to context
                    contentDescription = UiStringText(
                            ReaderUtils.getLongLikeLabelText(
                                    WordPress.getContext(),
                                    post.numLikes,
                                    post.isLikedByCurrentUser
                            )
                    ),
                    onClicked = if (accountStore.hasAccessToken()) onClicked else null
            )
        } else {
            ActionUiState(isEnabled = false)
        }
    }

    private fun buildReblogSection(
        post: ReaderPost,
        onReblogClicked: (Long, Long, Boolean) -> Unit
    ): ActionUiState {
        val canReblog = !post.isPrivate && accountStore.hasAccessToken()
        return if (canReblog) {
            // TODO Add content description
            ActionUiState(isEnabled = true, onClicked = onReblogClicked)
        } else {
            ActionUiState(isEnabled = false)
        }
    }

    private fun buildCommentsSection(
        post: ReaderPost,
        isBookmarkList: Boolean,
        onCommentsClicked: (Long, Long, Boolean) -> Unit
    ): ActionUiState {
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
            ActionUiState(
                    isEnabled = true,
                    count = post.numReplies,
                    onClicked = onCommentsClicked
            )
        } else {
            ActionUiState(isEnabled = false)
        }
    }
}
