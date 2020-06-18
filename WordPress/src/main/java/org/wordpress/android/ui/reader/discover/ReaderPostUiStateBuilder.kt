package org.wordpress.android.ui.reader.discover

import dagger.Reusable
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
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.ReaderCardUiState.ReaderPostUiState.DiscoverLayoutUiState
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.GravatarUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.image.ImageType.AVATAR
import org.wordpress.android.util.image.ImageType.BLAVATAR
import java.lang.IllegalStateException

@Reusable
class ReaderPostUiStateBuilder constructor(private val accountStore: AccountStore) {
    fun mapPostToUiState(post: ReaderPost, photonWidth: Int, photonHeight: Int): ReaderPostUiState {
        val blogUrl = post.takeIf { it.hasBlogUrl() }?.blogUrl?.let {
            // TODO malinjir remove static access
            UrlUtils.removeScheme(it)
        }

        // TODO malinjir remove static access
        val dateLine = DateTimeUtils.javaDateToTimeSpan(post.displayDate, WordPress.getContext())
        val avatarOrBlavatarUrl = post.takeIf { it.hasBlogImageUrl() }?.blogImageUrl
                // TODO malinjir remove static access + use R.dimen.avatar_sz_medium
                ?.let { GravatarUtils.fixGravatarUrl(it, 9999) }
        val blogName = post.takeIf { it.hasBlogName() }?.blogName
        val excerpt = post.takeIf { post.cardType != PHOTO && post.hasExcerpt() }?.excerpt
        val title = post.takeIf { post.cardType != PHOTO && it.hasTitle() }?.title
        // TODO malinjir `post.cardType != GALLERY` might not be needed
        val photoFrameVisibility = (post.hasFeaturedVideo() || post.hasFeaturedImage())
                && post.cardType != GALLERY
        val photoTitle = post.takeIf { it.cardType == PHOTO && it.hasTitle() }?.title
        val featuredImageUrl = post
                // TODO malinjir can we just check hasFeaturedImage or can it return true for video and gallery types?
                .takeIf { (it.cardType == PHOTO || it.cardType == DEFAULT) && it.hasFeaturedImage() }
                ?.getFeaturedImageForDisplay(photonWidth, photonHeight)
        val thumbnailStripUrls = post.takeIf { it.cardType == GALLERY }?.let { retrieveGalleryThumbnailUrls() }
        val videoOverlayVisbility = post.cardType == VIDEO
        val videoThumbnailUrl = post.takeIf { post.cardType == VIDEO }?.let { retrieveVideoThumbnailUrl() }
        // TODO malinjir Consider adding `postListType == ReaderPostListType.TAG_FOLLOWED` to showMoreMenu
        val showMoreMenu = accountStore.hasAccessToken()
        val discoverSection = post.takeIf { post.isDiscoverPost && post.discoverData.discoverType != OTHER }
                ?.let { buildDiscoverSectionUiState(post.discoverData) }

        // TODO malinjir onPostContainer click
        // TODO malinjir on item rendered callback -> handle load more event and trackRailcarRender
        // TODO malinjir bookmark action
        // TODO malinjir reblog action
        // TODO malinjir comments action
        // TODO malinjir likes action

        return ReaderPostUiState(
                post.postId,
                title = title,
                excerpt = excerpt,
                blogUrl = blogUrl,
                blogName = blogName,
                dateLine = dateLine,
                avatarOrBlavatarUrl = avatarOrBlavatarUrl,
                photoFrameVisibility = photoFrameVisibility,
                photoTitle = photoTitle,
                featuredImageUrl = featuredImageUrl,
                thumbnailStripUrls = thumbnailStripUrls,
                videoOverlayVisbility = videoOverlayVisbility,
                moreMenuVisbility = showMoreMenu,
                discoverSection = discoverSection,
                videoThumbnailUrl = videoThumbnailUrl
        )
    }

    private fun buildDiscoverSectionUiState(discoverData: ReaderPostDiscoverData): DiscoverLayoutUiState {
        // TODO malinjir don't store Spanned in VM/UiState => refactor getAttributionHtml method.
        val discoverText = discoverData.attributionHtml
        // TODO malinjir remove static access + use R.dimen.avatar_sz_small
        val discoverAvatarUrl = GravatarUtils.fixGravatarUrl(discoverData.avatarUrl, 9999)
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