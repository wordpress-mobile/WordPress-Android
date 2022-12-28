package org.wordpress.android.ui.reader

import dagger.Reusable
import org.wordpress.android.R
import org.wordpress.android.R.dimen
import org.wordpress.android.R.string
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.SecondaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.discover.ReaderPostUiStateBuilder
import org.wordpress.android.ui.reader.models.ReaderSimplePost
import org.wordpress.android.ui.reader.models.ReaderSimplePostList
import org.wordpress.android.ui.reader.utils.FeaturedImageUtils
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.utils.ThreadedCommentsUtils
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.CommentSnippetState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.CommentSnippetState.CommentSnippetData
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.CommentSnippetState.Empty
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.CommentSnippetState.Failure
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.CommentSnippetState.Loading
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.CommentSnippetUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState.ExcerptFooterUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState.ReaderPostFeaturedImageUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState.RelatedPostsUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState.RelatedPostsUiState.ReaderRelatedPostUiState
import org.wordpress.android.ui.reader.views.ReaderPostDetailsHeaderViewUiStateBuilder
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState.ButtonState
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState.CommentState
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState.LoadingState
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState.TextMessage
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.HtmlUtilsWrapper
import org.wordpress.android.ui.utils.UiDimen.UIDimenRes
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.GravatarUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

const val READER_POST_FEATURED_IMAGE_HEIGHT_PERCENT = 0.4
const val RELATED_POST_IMAGE_HEIGHT_WIDTH_RATION = 0.56 // 9:16

@Reusable
class ReaderPostDetailUiStateBuilder @Inject constructor(
    private val postDetailsHeaderViewUiStateBuilder: ReaderPostDetailsHeaderViewUiStateBuilder,
    private val postUiStateBuilder: ReaderPostUiStateBuilder,
    private val featuredImageUtils: FeaturedImageUtils,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val displayUtilsWrapper: DisplayUtilsWrapper,
    private val contextProvider: ContextProvider,
    private val htmlUtilsWrapper: HtmlUtilsWrapper,
    private val htmlMessageUtils: HtmlMessageUtils,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val gravatarUtilsWrapper: GravatarUtilsWrapper,
    private val threadedCommentsUtils: ThreadedCommentsUtils,
    resourceProvider: ResourceProvider
) {
    private val relatedPostFeaturedImageWidth: Int = resourceProvider
        .getDimensionPixelSize(R.dimen.reader_related_post_image_width)
    private val relatedPostFeaturedImageHeight: Int = (relatedPostFeaturedImageWidth
            * RELATED_POST_IMAGE_HEIGHT_WIDTH_RATION).toInt()

    fun mapPostToUiState(
        post: ReaderPost,
        moreMenuItems: List<SecondaryAction>? = null,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit,
        onBlogSectionClicked: (Long, Long) -> Unit,
        onFollowClicked: () -> Unit,
        onTagItemClicked: (String) -> Unit
    ) = ReaderPostDetailsUiState(
        postId = post.postId,
        blogId = post.blogId,
        featuredImageUiState = buildReaderPostFeaturedImageUiState(post),
        headerUiState = buildPostDetailsHeaderUiState(
            post,
            onBlogSectionClicked,
            onFollowClicked,
            onTagItemClicked
        ),
        excerptFooterUiState = buildExcerptFooterUiState(post),
        moreMenuItems = moreMenuItems,
        actions = buildPostActions(post, onButtonClicked)
    )

    fun mapRelatedPostsToUiState(
        sourcePost: ReaderPost,
        relatedPosts: ReaderSimplePostList,
        isGlobal: Boolean,
        onItemClicked: (Long, Long, Boolean) -> Unit
    ) = RelatedPostsUiState(
        cards = relatedPosts.map {
            mapRelatedPostToUiState(
                post = it,
                isGlobal = isGlobal,
                onItemClicked = onItemClicked
            )
        },
        isGlobal = isGlobal,
        headerLabel = buildRelatedPostsHeaderLabel(blogName = sourcePost.blogName, isGlobal = isGlobal),
        railcarJsonStrings = relatedPosts.map { it.railcarJson }
    )

    fun buildCommentSnippetUiState(
        commentSnippetState: CommentSnippetState,
        post: ReaderPost?,
        onCommentSnippetClicked: (Long, Long) -> Unit
    ): CommentSnippetUiState {
        return post?.let { readerPost ->
            AppLog.d(T.READER, "buildCommentSnippetUiState -> post was not null")

            CommentSnippetUiState(
                commentsNumber = readerPost.numReplies,
                showFollowConversation = readerPost.isWP && readerPost.isCommentsOpen,
                snippetItems = getSnippetItems(commentSnippetState, post, onCommentSnippetClicked)
            )
        } ?: run {
            AppLog.d(T.READER, "buildCommentSnippetUiState -> post was null")
            CommentSnippetUiState(
                snippetItems = listOf(LoadingState),
                showFollowConversation = false,
                commentsNumber = 0
            )
        }
    }

    private fun getSnippetItems(
        commentSnippetState: CommentSnippetState,
        readerPost: ReaderPost,
        onCommentSnippetClicked: (Long, Long) -> Unit
    ): List<CommentSnippetItemState> {
        return when (commentSnippetState) {
            is CommentSnippetData -> commentSnippetState.comments.map { readerComment ->
                CommentState(
                    authorName = readerComment.authorName,
                    datePublished = dateTimeUtilsWrapper.javaDateToTimeSpan(
                        dateTimeUtilsWrapper.dateFromIso8601(
                            readerComment.published
                        )
                    ),
                    avatarUrl = gravatarUtilsWrapper.fixGravatarUrl(
                        readerComment.authorAvatar,
                        contextProvider.getContext().resources.getDimensionPixelSize(
                            dimen.avatar_sz_extra_small
                        )
                    ),
                    showAuthorBadge = readerComment.authorId == readerPost.authorId,
                    commentText = readerComment.text,
                    isPrivatePost = threadedCommentsUtils.isPrivatePost(readerPost),
                    blogId = readerComment.blogId,
                    postId = readerComment.postId,
                    commentId = readerComment.commentId
                )
            } + ButtonState(
                buttonText = UiStringRes(string.reader_comments_view_all),
                postId = readerPost.postId,
                blogId = readerPost.blogId,
                onCommentSnippetClicked = onCommentSnippetClicked
            )
            is Empty -> {
                listOf<CommentSnippetItemState>(
                    TextMessage(commentSnippetState.message)
                ) + if (readerPost.isCommentsOpen) {
                    listOf<CommentSnippetItemState>(
                        ButtonState(
                            buttonText = UiStringRes(string.reader_comments_be_first_to_comment),
                            postId = readerPost.postId,
                            blogId = readerPost.blogId,
                            onCommentSnippetClicked = onCommentSnippetClicked
                        )
                    )
                } else {
                    listOf<CommentSnippetItemState>()
                }
            }
            is Failure -> listOf(
                TextMessage(commentSnippetState.message),
                ButtonState(
                    buttonText = UiStringRes(string.reader_comments_view_all),
                    postId = readerPost.postId,
                    blogId = readerPost.blogId,
                    onCommentSnippetClicked = onCommentSnippetClicked
                )
            )
            Loading -> listOf(LoadingState)
        }
    }

    private fun mapRelatedPostToUiState(
        post: ReaderSimplePost,
        isGlobal: Boolean,
        onItemClicked: (Long, Long, Boolean) -> Unit
    ) = ReaderRelatedPostUiState(
        postId = post.postId,
        blogId = post.siteId,
        isGlobal = isGlobal,
        title = post.takeIf { it.hasTitle() }?.let { UiStringText(it.title) },
        excerpt = post.takeIf { it.hasExcerpt() }?.let { UiStringText(it.excerpt) },
        featuredImageUrl = buildFeaturedImageUrl(
            post,
            relatedPostFeaturedImageWidth,
            relatedPostFeaturedImageHeight
        ),
        featuredImageVisibility = post.featuredImageUrl?.isNotEmpty() == true,
        featuredImageCornerRadius = UIDimenRes(R.dimen.reader_featured_image_corner_radius),
        onItemClicked = onItemClicked
    )

    private fun buildReaderPostFeaturedImageUiState(post: ReaderPost) =
        post.takeIf { featuredImageUtils.shouldAddFeaturedImage(post) }?.let {
            ReaderPostFeaturedImageUiState(
                blogId = post.blogId,
                url = buildReaderPostFeaturedImageUrl(post),
                height = (displayUtilsWrapper.getWindowPixelHeight() *
                        READER_POST_FEATURED_IMAGE_HEIGHT_PERCENT).toInt()
            )
        }

    private fun buildReaderPostFeaturedImageUrl(post: ReaderPost) = readerUtilsWrapper.getResizedImageUrl(
        post.featuredImage,
        displayUtilsWrapper.getDisplayPixelWidth(),
        0,
        post.isPrivate,
        post.isPrivateAtomic
    )

    private fun buildPostDetailsHeaderUiState(
        post: ReaderPost,
        onBlogSectionClicked: (Long, Long) -> Unit,
        onFollowClicked: () -> Unit,
        onTagItemClicked: (String) -> Unit
    ) = postDetailsHeaderViewUiStateBuilder.mapPostToUiState(
        post,
        onBlogSectionClicked,
        onFollowClicked,
        onTagItemClicked
    )

    private fun buildExcerptFooterUiState(post: ReaderPost): ExcerptFooterUiState? =
        post.takeIf { post.shouldShowExcerpt() }?.let {
            ExcerptFooterUiState(
                visitPostExcerptFooterLinkText = buildPostExcerptFooterLinkText(post),
                postLink = post.url
            )
        }

    private fun buildPostExcerptFooterLinkText(post: ReaderPost) = UiStringText(
        htmlMessageUtils.getHtmlMessageFromStringFormatResId(
            R.string.reader_excerpt_link,
            "<font color='" +
                    htmlUtilsWrapper
                        .colorResToHtmlColor(contextProvider.getContext(), R.color.link_reader) + "'>" +
                    post.blogName + "</font>"
        )
    )

    fun buildPostActions(
        post: ReaderPost,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ) = postUiStateBuilder.mapPostToActions(post, onButtonClicked)

    private fun buildRelatedPostsHeaderLabel(blogName: String, isGlobal: Boolean): UiString {
        return if (isGlobal) {
            UiStringRes(R.string.reader_label_global_related_posts)
        } else {
            UiStringResWithParams(R.string.reader_label_local_related_posts, listOf(UiStringText(blogName)))
        }
    }

    private fun buildFeaturedImageUrl(post: ReaderSimplePost, imageWidth: Int, imageHeight: Int) =
        post.getFeaturedImageForDisplay(imageWidth, imageHeight)
}
