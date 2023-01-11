package org.wordpress.android.ui.reader.discover

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderCardType.DEFAULT
import org.wordpress.android.models.ReaderCardType.GALLERY
import org.wordpress.android.models.ReaderCardType.PHOTO
import org.wordpress.android.models.ReaderCardType.VIDEO
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostDiscoverData
import org.wordpress.android.models.ReaderPostDiscoverData.DiscoverType.EDITOR_PICK
import org.wordpress.android.models.ReaderPostDiscoverData.DiscoverType.OTHER
import org.wordpress.android.models.ReaderPostDiscoverData.DiscoverType.SITE_PICK
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestChipStyleColor
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState.ChipStyle
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState.ChipStyle.ChipStyleGreen
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState.ChipStyle.ChipStyleOrange
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState.ChipStyle.ChipStylePurple
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState.ChipStyle.ChipStyleYellow
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderInterestsCardUiState.ReaderInterestUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState.DiscoverLayoutUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState.GalleryThumbnailStripData
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState.ReaderRecommendedBlogUiState
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.SecondaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BOOKMARK
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.LIKE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REBLOG
import org.wordpress.android.ui.reader.utils.ReaderImageScannerProvider
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState.ReaderBlogSectionClickData
import org.wordpress.android.ui.utils.UiDimen.UIDimenRes
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.GravatarUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.UrlUtilsWrapper
import org.wordpress.android.util.image.BlavatarShape.CIRCULAR
import org.wordpress.android.util.image.ImageType
import javax.inject.Inject
import javax.inject.Named

private const val READER_INTEREST_LIST_SIZE_LIMIT = 5
private const val READER_RECOMMENDED_BLOGS_LIST_SIZE_LIMIT = 3

@Reusable
class ReaderPostUiStateBuilder @Inject constructor(
    private val accountStore: AccountStore,
    private val urlUtilsWrapper: UrlUtilsWrapper,
    private val gravatarUtilsWrapper: GravatarUtilsWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val readerImageScannerProvider: ReaderImageScannerProvider,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val readerPostTagsUiStateBuilder: ReaderPostTagsUiStateBuilder,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    @Suppress("LongParameterList")
    suspend fun mapPostToUiState(
        source: String,
        post: ReaderPost,
        isDiscover: Boolean = false,
        photonWidth: Int,
        photonHeight: Int,
        postListType: ReaderPostListType,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit,
        onItemClicked: (Long, Long) -> Unit,
        onItemRendered: (ReaderCardUiState) -> Unit,
        onDiscoverSectionClicked: (Long, Long) -> Unit,
        onMoreButtonClicked: (ReaderPostUiState) -> Unit,
        onMoreDismissed: (ReaderPostUiState) -> Unit,
        onVideoOverlayClicked: (Long, Long) -> Unit,
        onPostHeaderViewClicked: (Long, Long) -> Unit,
        onTagItemClicked: (String) -> Unit,
        moreMenuItems: List<SecondaryAction>? = null
    ): ReaderPostUiState {
        return withContext(bgDispatcher) {
            mapPostToUiStateBlocking(
                source,
                post,
                isDiscover,
                photonWidth,
                photonHeight,
                postListType,
                onButtonClicked,
                onItemClicked,
                onItemRendered,
                onDiscoverSectionClicked,
                onMoreButtonClicked,
                onMoreDismissed,
                onVideoOverlayClicked,
                onPostHeaderViewClicked,
                onTagItemClicked,
                moreMenuItems
            )
        }
    }

    @Suppress("LongParameterList")
    fun mapPostToUiStateBlocking(
        source: String,
        post: ReaderPost,
        isDiscover: Boolean = false,
        photonWidth: Int,
        photonHeight: Int,
        postListType: ReaderPostListType,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit,
        onItemClicked: (Long, Long) -> Unit,
        onItemRendered: (ReaderCardUiState) -> Unit,
        onDiscoverSectionClicked: (Long, Long) -> Unit,
        onMoreButtonClicked: (ReaderPostUiState) -> Unit,
        onMoreDismissed: (ReaderPostUiState) -> Unit,
        onVideoOverlayClicked: (Long, Long) -> Unit,
        onPostHeaderViewClicked: (Long, Long) -> Unit,
        onTagItemClicked: (String) -> Unit,
        moreMenuItems: List<ReaderPostCardAction>? = null
    ): ReaderPostUiState {
        return ReaderPostUiState(
            source = source,
            postId = post.postId,
            blogId = post.blogId,
            feedId = post.feedId,
            isFollowed = post.isFollowedByCurrentUser,
            blogSection = buildBlogSection(post, onPostHeaderViewClicked, postListType, post.isP2orA8C),
            excerpt = buildExcerpt(post),
            title = buildTitle(post),
            tagItems = buildTagItems(post, onTagItemClicked),
            photoFrameVisibility = buildPhotoFrameVisibility(post),
            photoTitle = buildPhotoTitle(post),
            featuredImageUrl = buildFeaturedImageUrl(post, photonWidth, photonHeight),
            featuredImageCornerRadius = UIDimenRes(R.dimen.reader_featured_image_corner_radius),
            thumbnailStripSection = buildThumbnailStripUrls(post),
            expandableTagsViewVisibility = buildExpandedTagsViewVisibility(post, isDiscover),
            videoOverlayVisibility = buildVideoOverlayVisibility(post),
            featuredImageVisibility = buildFeaturedImageVisibility(post),
            moreMenuVisibility = accountStore.hasAccessToken(),
            moreMenuItems = moreMenuItems,
            fullVideoUrl = buildFullVideoUrl(post),
            discoverSection = buildDiscoverSection(post, onDiscoverSectionClicked),
            bookmarkAction = buildBookmarkSection(post, onButtonClicked),
            likeAction = buildLikeSection(post, onButtonClicked),
            reblogAction = buildReblogSection(post, onButtonClicked),
            commentsAction = buildCommentsSection(post, onButtonClicked),
            onItemClicked = onItemClicked,
            onItemRendered = onItemRendered,
            onMoreButtonClicked = onMoreButtonClicked,
            onMoreDismissed = onMoreDismissed,
            onVideoOverlayClicked = onVideoOverlayClicked
        )
    }

    fun mapPostToBlogSectionUiState(
        post: ReaderPost,
        onBlogSectionClicked: (Long, Long) -> Unit
    ): ReaderBlogSectionUiState {
        return buildBlogSection(post, onBlogSectionClicked)
    }

    fun mapPostToActions(
        post: ReaderPost,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ): ReaderPostActions {
        return ReaderPostActions(
            bookmarkAction = buildBookmarkSection(post, onButtonClicked),
            likeAction = buildLikeSection(post, onButtonClicked),
            reblogAction = buildReblogSection(post, onButtonClicked),
            commentsAction = buildCommentsSection(post, onButtonClicked)
        )
    }

    suspend fun mapTagListToReaderInterestUiState(
        interests: ReaderTagList,
        onClicked: ((String) -> Unit)
    ): ReaderInterestsCardUiState {
        return withContext(bgDispatcher) {
            val listSize = if (interests.size < READER_INTEREST_LIST_SIZE_LIMIT) {
                interests.size
            } else {
                READER_INTEREST_LIST_SIZE_LIMIT
            }

            return@withContext ReaderInterestsCardUiState(interests.take(listSize).map { interest ->
                ReaderInterestUiState(
                    interest = interest.tagTitle,
                    onClicked = onClicked,
                    chipStyle = buildChipStyle(interest, interests)
                )
            })
        }
    }

    suspend fun mapRecommendedBlogsToReaderRecommendedBlogsCardUiState(
        recommendedBlogs: List<ReaderBlog>,
        onItemClicked: (Long, Long, Boolean) -> Unit,
        onFollowClicked: (ReaderRecommendedBlogUiState) -> Unit
    ): ReaderRecommendedBlogsCardUiState = withContext(bgDispatcher) {
        recommendedBlogs.take(READER_RECOMMENDED_BLOGS_LIST_SIZE_LIMIT)
            .map {
                ReaderRecommendedBlogUiState(
                    name = it.name,
                    url = urlUtilsWrapper.removeScheme(it.url),
                    blogId = it.blogId,
                    feedId = it.feedId,
                    description = it.description.ifEmpty { null },
                    iconUrl = it.imageUrl,
                    isFollowed = it.isFollowing,
                    onFollowClicked = onFollowClicked,
                    onItemClicked = onItemClicked
                )
            }.let { ReaderRecommendedBlogsCardUiState(it) }
    }

    private fun buildBlogSection(
        post: ReaderPost,
        onBlogSectionClicked: (Long, Long) -> Unit,
        postListType: ReaderPostListType? = null,
        isP2Post: Boolean = false
    ) = buildBlogSectionUiState(post, onBlogSectionClicked, postListType, isP2Post)

    private fun buildBlogSectionUiState(
        post: ReaderPost,
        onBlogSectionClicked: (Long, Long) -> Unit,
        postListType: ReaderPostListType?,
        isP2Post: Boolean = false
    ): ReaderBlogSectionUiState {
        return ReaderBlogSectionUiState(
            postId = post.postId,
            blogId = post.blogId,
            blogName = buildBlogName(post, isP2Post),
            blogUrl = buildBlogUrl(post),
            dateLine = buildDateLine(post),
            avatarOrBlavatarUrl = buildAvatarOrBlavatarUrl(post),
            isAuthorAvatarVisible = isP2Post,
            blavatarType = SiteUtils.getSiteImageType(isP2Post, CIRCULAR),
            authorAvatarUrl = gravatarUtilsWrapper.fixGravatarUrlWithResource(
                post.postAvatar,
                R.dimen.avatar_sz_medium
            ),
            blogSectionClickData = buildOnBlogSectionClicked(onBlogSectionClicked, postListType)
        )
    }

    private fun buildOnBlogSectionClicked(
        onBlogSectionClicked: (Long, Long) -> Unit,
        postListType: ReaderPostListType?
    ): ReaderBlogSectionClickData? {
        return if (postListType != ReaderPostListType.BLOG_PREVIEW) {
            ReaderBlogSectionClickData(onBlogSectionClicked, android.R.attr.selectableItemBackground)
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

    private fun buildExpandedTagsViewVisibility(post: ReaderPost, isDiscover: Boolean) =
        post.tags.isNotEmpty() && isDiscover

    private fun buildTagItems(post: ReaderPost, onClicked: (String) -> Unit) =
        readerPostTagsUiStateBuilder.mapPostTagsToTagUiStates(post, onClicked)

    // TODO malinjir show overlay when buildFullVideoUrl != null
    private fun buildVideoOverlayVisibility(post: ReaderPost) = post.cardType == VIDEO

    private fun buildFeaturedImageVisibility(post: ReaderPost) =
        (post.cardType == PHOTO || post.cardType == DEFAULT) && post.hasFeaturedImage() ||
                post.cardType == VIDEO && post.hasFeaturedVideo()

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

    // TODO malinjir show title only when buildPhotoTitle == null
    private fun buildTitle(post: ReaderPost): UiString? {
        return if (post.cardType != PHOTO) {
            post.takeIf { it.hasTitle() }?.title?.let { UiStringText(it) }
                ?: UiStringRes(R.string.untitled_in_parentheses)
        } else {
            null
        }
    }

    // TODO malinjir show excerpt only when buildPhotoTitle == null
    private fun buildExcerpt(post: ReaderPost) =
        post.takeIf { post.cardType != PHOTO && post.hasExcerpt() }?.excerpt

    private fun buildBlogName(post: ReaderPost, isP2Post: Boolean = false): UiString {
        val blogName = post.takeIf { it.hasBlogName() }?.blogName?.let { UiStringText(it) }
            ?: UiStringRes(R.string.untitled_in_parentheses)

        if (!isP2Post) {
            return blogName
        }

        val authorName = if (post.hasAuthorFirstName()) {
            UiStringText(post.authorFirstName)
        } else {
            UiStringText(post.authorName)
        }

        return UiStringResWithParams(R.string.reader_author_with_blog_name, listOf(authorName, blogName))
    }

    private fun buildAvatarOrBlavatarUrl(post: ReaderPost) =
        post.takeIf { it.hasBlogImageUrl() }
            ?.blogImageUrl
            ?.let { gravatarUtilsWrapper.fixGravatarUrlWithResource(it, R.dimen.avatar_sz_medium) }

    private fun buildDateLine(post: ReaderPost) =
        dateTimeUtilsWrapper.javaDateToTimeSpan(post.getDisplayDate(dateTimeUtilsWrapper))

    @Suppress("UseCheckOrError")
    private fun buildDiscoverSectionUiState(
        discoverData: ReaderPostDiscoverData,
        onDiscoverSectionClicked: (Long, Long) -> Unit
    ): DiscoverLayoutUiState {
        val discoverText = discoverData.attributionHtml
        val discoverAvatarUrl = gravatarUtilsWrapper.fixGravatarUrlWithResource(
            discoverData.avatarUrl,
            R.dimen.avatar_sz_small
        )
        @Suppress("DEPRECATION") val discoverAvatarImageType = when (discoverData.discoverType) {
            EDITOR_PICK -> ImageType.AVATAR
            SITE_PICK -> ImageType.BLAVATAR
            OTHER -> throw IllegalStateException("This could should be unreachable.")
            else -> ImageType.AVATAR
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
        onClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ): PrimaryAction {
        val contentDescription = UiStringRes(
            if (post.isBookmarked) {
                R.string.reader_remove_bookmark
            } else {
                R.string.reader_add_bookmark
            }
        )
        return if (post.postId != 0L && post.blogId != 0L) {
            PrimaryAction(
                isEnabled = true,
                isSelected = post.isBookmarked,
                contentDescription = contentDescription,
                onClicked = onClicked,
                type = BOOKMARK
            )
        } else {
            PrimaryAction(isEnabled = false, contentDescription = contentDescription, type = BOOKMARK)
        }
    }

    private fun buildLikeSection(
        post: ReaderPost,
        onClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ): PrimaryAction {
        val likesEnabled = post.canLikePost() && accountStore.hasAccessToken()

        return PrimaryAction(
            isEnabled = likesEnabled,
            isSelected = post.isLikedByCurrentUser,
            contentDescription = UiStringText(
                readerUtilsWrapper.getLongLikeLabelText(post.numLikes, post.isLikedByCurrentUser)
            ),
            count = post.numLikes,
            onClicked = if (likesEnabled) onClicked else null,
            type = LIKE
        )
    }

    private fun buildReblogSection(
        post: ReaderPost,
        onReblogClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ): PrimaryAction {
        val canReblog = !post.isPrivate && accountStore.hasAccessToken()
        return PrimaryAction(
            isEnabled = canReblog,
            contentDescription = UiStringRes(R.string.reader_view_reblog),
            onClicked = if (canReblog) onReblogClicked else null,
            type = REBLOG
        )
    }

    private fun buildCommentsSection(
        post: ReaderPost,
        onCommentsClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ): PrimaryAction {
        val showComments = when {
            post.isDiscoverPost -> false
            !accountStore.hasAccessToken() -> post.numReplies > 0
            else -> post.isWP && (post.isCommentsOpen || post.numReplies > 0)
        }
        val contentDescription = UiStringRes(R.string.comments)

        return if (showComments) {
            PrimaryAction(
                isEnabled = true,
                count = post.numReplies,
                contentDescription = contentDescription,
                onClicked = onCommentsClicked,
                type = ReaderPostCardActionType.COMMENTS
            )
        } else {
            PrimaryAction(
                isEnabled = false,
                contentDescription = contentDescription,
                type = ReaderPostCardActionType.COMMENTS
            )
        }
    }

    private fun buildChipStyle(readerTag: ReaderTag, readerTagList: ReaderTagList): ChipStyle {
        val colorCount = ReaderInterestChipStyleColor.values().size
        val index = readerTagList.indexOf(readerTag)

        return when (index % colorCount) {
            ReaderInterestChipStyleColor.GREEN.id -> ChipStyleGreen
            ReaderInterestChipStyleColor.PURPLE.id -> ChipStylePurple
            ReaderInterestChipStyleColor.YELLOW.id -> ChipStyleYellow
            ReaderInterestChipStyleColor.ORANGE.id -> ChipStyleOrange
            else -> ChipStyleGreen
        }
    }
}
