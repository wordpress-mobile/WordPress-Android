package org.wordpress.android.ui.reader.discover

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
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.ReaderCardUiState.ReaderPostUiState.DiscoverLayoutUiState
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
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper
) {
    fun mapPostToUiState(post: ReaderPost, photonWidth: Int, photonHeight: Int): ReaderPostUiState {
        // TODO malinjir onPostContainer click
        // TODO malinjir on item rendered callback -> handle load more event and trackRailcarRender
        // TODO malinjir bookmark action
        // TODO malinjir reblog action
        // TODO malinjir comments action
        // TODO malinjir likes action

        return ReaderPostUiState(
                id = post.postId,
                blogUrl = buildBlogUrl(post),
                dateLine = buildDateLine(post),
                avatarOrBlavatarUrl = buildAvatarOrBlavatarUrl(post),
                blogName = buildBlogName(post),
                excerpt = buildExcerpt(post),
                title = buildTitle(post),
                photoFrameVisibility = buildPhotoFrameVisbility(post),
                photoTitle = buildPhotoTitle(post),
                featuredImageUrl = buildFeaturedImageUrl(post, photonWidth, photonHeight),
                thumbnailStripUrls = buildThumbnailStripUrls(post),
                videoOverlayVisbility = buildVideoOverlayVisbility(post),
                // TODO malinjir Consider adding `postListType == ReaderPostListType.TAG_FOLLOWED` to showMoreMenu
                moreMenuVisbility = accountStore.hasAccessToken(),
                videoThumbnailUrl = buildVideoThumbnailUrl(post),
                discoverSection = buildDiscoverSection(post)
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

    private fun buildVideoOverlayVisbility(post: ReaderPost) = post.cardType == VIDEO

    private fun buildThumbnailStripUrls(post: ReaderPost) =
            post.takeIf { it.cardType == GALLERY }
                    ?.let { retrieveGalleryThumbnailUrls() }

    private fun buildFeaturedImageUrl(post: ReaderPost, photonWidth: Int, photonHeight: Int): String? {
        return post
                .takeIf { (it.cardType == PHOTO || it.cardType == DEFAULT) && it.hasFeaturedImage() }
                ?.getFeaturedImageForDisplay(photonWidth, photonHeight)
    }

    private fun buildPhotoTitle(post: ReaderPost) =
            post.takeIf { it.cardType == PHOTO && it.hasTitle() }?.title

    // TODO malinjir `post.cardType != GALLERY` might not be needed
    private fun buildPhotoFrameVisbility(post: ReaderPost) =
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

    private fun retrieveGalleryThumbnailUrls(): List<String> {
        // TODO malinjir Not yet implemented - Refactor ReaderThumbnailStrip.loadThumbnails()
        return emptyList()
    }
}
