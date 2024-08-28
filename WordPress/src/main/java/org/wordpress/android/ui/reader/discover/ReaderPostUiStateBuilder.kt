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
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState.InteractionSectionData
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
import org.wordpress.android.util.WPAvatarUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.UrlUtilsWrapper
import org.wordpress.android.util.image.BlavatarShape.CIRCULAR
import javax.inject.Inject
import javax.inject.Named
import android.R as AndroidR

private const val READER_INTEREST_LIST_SIZE_LIMIT = 5
private const val READER_RECOMMENDED_BLOGS_LIST_SIZE_LIMIT = 3

@Reusable
class ReaderPostUiStateBuilder @Inject constructor(
    private val accountStore: AccountStore,
    private val urlUtilsWrapper: UrlUtilsWrapper,
    private val avatarUtilsWrapper: WPAvatarUtilsWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val readerImageScannerProvider: ReaderImageScannerProvider,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    @Suppress("LongParameterList")
    suspend fun mapPostToUiState(
        source: String,
        post: ReaderPost,
        photonWidth: Int,
        photonHeight: Int,
        postListType: ReaderPostListType,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit,
        onItemClicked: (Long, Long) -> Unit,
        onItemRendered: (ReaderCardUiState) -> Unit,
        onMoreButtonClicked: (ReaderPostUiState) -> Unit,
        onMoreDismissed: (ReaderPostUiState) -> Unit,
        onVideoOverlayClicked: (Long, Long) -> Unit,
        onPostHeaderViewClicked: (Long, Long) -> Unit,
        moreMenuItems: List<SecondaryAction>? = null,
    ): ReaderPostUiState {
        return withContext(bgDispatcher) {
            mapPostToUiStateBlocking(
                source,
                post,
                photonWidth,
                photonHeight,
                postListType,
                onButtonClicked,
                onItemClicked,
                onItemRendered,
                onMoreButtonClicked,
                onMoreDismissed,
                onVideoOverlayClicked,
                onPostHeaderViewClicked,
                moreMenuItems,
            )
        }
    }

    @Suppress("LongParameterList")
    fun mapPostToUiStateBlocking(
        source: String,
        post: ReaderPost,
        photonWidth: Int,
        photonHeight: Int,
        postListType: ReaderPostListType,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit,
        onItemClicked: (Long, Long) -> Unit,
        onItemRendered: (ReaderCardUiState) -> Unit,
        onMoreButtonClicked: (ReaderPostUiState) -> Unit,
        onMoreDismissed: (ReaderPostUiState) -> Unit,
        onVideoOverlayClicked: (Long, Long) -> Unit,
        onPostHeaderViewClicked: (Long, Long) -> Unit,
        moreMenuItems: List<ReaderPostCardAction>? = null,
    ): ReaderPostUiState {
        return ReaderPostUiState(
            source = source,
            postId = post.postId,
            blogId = post.blogId,
            feedId = post.feedId,
            isFollowed = post.isFollowedByCurrentUser,
            blogSection = buildCompactBlogSection(post, postListType, onPostHeaderViewClicked, post.isP2orA8C),
            interactionSection = buildInteractionSection(post),
            title = buildTitle(post),
            excerpt = buildExcerpt(post),
            featuredImageUrl = buildFeaturedImageUrl(post, photonWidth, photonHeight),
            featuredImageCornerRadius = UIDimenRes(R.dimen.reader_featured_image_corner_radius_new),
            fullVideoUrl = buildFullVideoUrl(post),
            thumbnailStripSection = buildThumbnailStripUrls(post),
            videoOverlayVisibility = buildVideoOverlayVisibility(post),
            featuredImageVisibility = buildFeaturedImageVisibility(post),
            moreMenuVisibility = accountStore.hasAccessToken(),
            likeAction = buildLikeSection(post, onButtonClicked, isReaderImprovementsEnabled = true),
            reblogAction = buildReblogSection(post, onButtonClicked),
            commentsAction = buildCommentsSection(post, onButtonClicked),
            moreMenuItems = moreMenuItems,
            onItemClicked = onItemClicked,
            onItemRendered = onItemRendered,
            onMoreButtonClicked = onMoreButtonClicked,
            onMoreDismissed = onMoreDismissed,
            onVideoOverlayClicked = onVideoOverlayClicked,
        )
    }

    fun mapPostToBlogSectionUiState(
        post: ReaderPost,
        onBlogSectionClicked: () -> Unit
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
                    slug = interest.tagSlug,
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
                    isFollowEnabled = true,
                    onFollowClicked = onFollowClicked,
                    onItemClicked = onItemClicked
                )
            }.let { ReaderRecommendedBlogsCardUiState(it) }
    }

    private fun buildBlogSection(
        post: ReaderPost,
        onBlogSectionClicked: () -> Unit,
        postListType: ReaderPostListType? = null,
        isP2Post: Boolean = false,
    ): ReaderBlogSectionUiState {
        return ReaderBlogSectionUiState(
            postId = post.postId,
            blogId = post.blogId,
            blogName = buildBlogName(post, isP2Post),
            blogUrl = buildBlogUrl(post),
            dateLine = buildDateLine(post),
            avatarOrBlavatarUrl = buildAvatarOrBlavatarUrl(post),
            isAuthorAvatarVisible = isP2Post || post.hasBlogImageUrl(),
            blavatarType = SiteUtils.getSiteImageType(isP2Post, CIRCULAR),
            authorAvatarUrl = avatarUtilsWrapper.rewriteAvatarUrlWithResource(
                post.postAvatar,
                R.dimen.avatar_sz_medium
            ),
            blogSectionClickData = buildOnBlogSectionClicked(onBlogSectionClicked, postListType)
        )
    }

    private fun buildCompactBlogSection(
        post: ReaderPost,
        postListType: ReaderPostListType,
        onBlogSectionClicked: (Long, Long) -> Unit,
        isP2Post: Boolean = false,
    ): ReaderPostUiState.CompactBlogSectionData {
        return ReaderPostUiState.CompactBlogSectionData(
            postId = post.postId,
            blogId = post.blogId,
            blogName = buildBlogName(post, isP2Post),
            dateLine = buildDateLine(post),
            avatarOrBlavatarUrl = buildAvatarOrBlavatarUrl(post),
            isAuthorAvatarVisible = isP2Post,
            blavatarType = SiteUtils.getSiteImageType(isP2Post, CIRCULAR),
            authorAvatarUrl = avatarUtilsWrapper.rewriteAvatarUrlWithResource(
                post.postAvatar,
                R.dimen.avatar_sz_medium
            ),
            onClicked = onBlogSectionClicked.takeIf { postListType != ReaderPostListType.BLOG_PREVIEW },
        )
    }

    private fun buildOnBlogSectionClicked(
        onBlogSectionClicked: () -> Unit,
        postListType: ReaderPostListType?
    ): ReaderBlogSectionClickData? {
        return if (postListType != ReaderPostListType.BLOG_PREVIEW) {
            ReaderBlogSectionClickData(onBlogSectionClicked, AndroidR.attr.selectableItemBackground)
        } else {
            null
        }
    }

    private fun buildInteractionSection(
        post: ReaderPost
    ): InteractionSectionData = InteractionSectionData(
        likeCount = post.numLikes,
        commentCount = post.numReplies,
    )

    private fun buildBlogUrl(post: ReaderPost) = post
        .takeIf { it.hasBlogUrl() }
        ?.blogUrl
        ?.let { urlUtilsWrapper.removeScheme(it) }

    private fun buildFullVideoUrl(post: ReaderPost) =
        post.takeIf { post.cardType == VIDEO }
            ?.let { post.featuredVideo }

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

    private fun buildTitle(post: ReaderPost): UiString? =
        post.takeIf { it.hasTitle() }?.title?.let { UiStringText(it) }

    private fun buildExcerpt(post: ReaderPost) =
        post.takeIf { post.hasExcerpt() }?.excerpt

    private fun buildBlogName(post: ReaderPost, isP2Post: Boolean = false): UiString {
        val blogName = post.takeIf { it.hasBlogName() }?.blogName?.let { UiStringText(it) }
            ?:post.takeIf { it.hasBlogUrl() }
            ?.blogUrl
            ?.let { UiStringText(urlUtilsWrapper.removeScheme(it)) }
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
            ?.let { avatarUtilsWrapper.rewriteAvatarUrlWithResource(it, R.dimen.avatar_sz_medium) }

    private fun buildDateLine(post: ReaderPost) =
        dateTimeUtilsWrapper.javaDateToTimeSpan(post.getDisplayDate(dateTimeUtilsWrapper))

    private fun retrieveGalleryThumbnailUrls(post: ReaderPost): ReaderPostUiState.GalleryThumbnailStripData {
        // scan post content for images suitable in a gallery
        val images = readerImageScannerProvider.createReaderImageScanner(post.text, post.isPrivate)
            .getImageList(ReaderConstants.THUMBNAIL_STRIP_IMG_COUNT, ReaderConstants.MIN_GALLERY_IMAGE_WIDTH)
        return ReaderPostUiState.GalleryThumbnailStripData(images, post.isPrivate, post.text)
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
        onClicked: (Long, Long, ReaderPostCardActionType) -> Unit,
        isReaderImprovementsEnabled: Boolean = false,
    ): PrimaryAction {
        val likesEnabled = post.canLikePost() && accountStore.hasAccessToken()

        val contentDescription = if (isReaderImprovementsEnabled) {
            UiStringRes(R.string.reader_label_like)
        } else {
            UiStringText(readerUtilsWrapper.getLongLikeLabelText(post.numLikes, post.isLikedByCurrentUser))
        }

        return PrimaryAction(
            isEnabled = likesEnabled,
            isSelected = post.isLikedByCurrentUser,
            contentDescription = contentDescription,
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
