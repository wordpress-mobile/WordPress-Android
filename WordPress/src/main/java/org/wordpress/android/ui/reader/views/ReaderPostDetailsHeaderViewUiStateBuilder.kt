package org.wordpress.android.ui.reader.views

import dagger.Reusable
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.discover.ReaderPostTagsUiStateBuilder
import org.wordpress.android.ui.reader.discover.ReaderPostUiStateBuilder
import org.wordpress.android.ui.reader.views.uistates.FollowButtonUiState
import org.wordpress.android.ui.reader.views.uistates.InteractionSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderAction
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderViewUiState.ReaderPostDetailsHeaderUiState
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtilsWrapper
import javax.inject.Inject

@Reusable
class ReaderPostDetailsHeaderViewUiStateBuilder @Inject constructor(
    private val accountStore: AccountStore,
    private val postUiStateBuilder: ReaderPostUiStateBuilder,
    private val readerPostTagsUiStateBuilder: ReaderPostTagsUiStateBuilder,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
) {
    fun mapPostToUiState(
        post: ReaderPost,
        onHeaderAction: (ReaderPostDetailsHeaderAction) -> Unit,
    ): ReaderPostDetailsHeaderUiState {
        val hasAccessToken = accountStore.hasAccessToken()
        val textTitle = post
            .takeIf { post.hasTitle() }
            ?.title?.let { UiStringText(it) }

        return ReaderPostDetailsHeaderUiState(
            title = textTitle,
            authorName = post.authorName,
            tagItems = buildTagItems(
                post,
                onClicked = { onHeaderAction(ReaderPostDetailsHeaderAction.TagItemClicked(it)) }
            ),
            tagItemsVisibility = buildTagItemsVisibility(post),
            blogSectionUiState = buildBlogSectionUiState(
                post,
                onBlogSectionClicked = { onHeaderAction(ReaderPostDetailsHeaderAction.BlogSectionClicked) }
            ),
            followButtonUiState = buildFollowButtonUiState(
                post,
                hasAccessToken,
                onFollowClicked = { onHeaderAction(ReaderPostDetailsHeaderAction.FollowClicked) }
            ),
            dateLine = buildDateLine(post),
            interactionSectionUiState = buildInteractionSection(
                post,
                onLikesClicked = { onHeaderAction(ReaderPostDetailsHeaderAction.LikesClicked) },
                onCommentsClicked = { onHeaderAction(ReaderPostDetailsHeaderAction.CommentsClicked) }
            )
        )
    }

    private fun buildBlogSectionUiState(
        post: ReaderPost,
        onBlogSectionClicked: () -> Unit
    ): ReaderBlogSectionUiState {
        return postUiStateBuilder.mapPostToBlogSectionUiState(
            post,
            onBlogSectionClicked
        )
    }

    private fun buildFollowButtonUiState(
        post: ReaderPost,
        hasAccessToken: Boolean,
        onFollowClicked: () -> Unit
    ): FollowButtonUiState {
        return FollowButtonUiState(
            onFollowButtonClicked = onFollowClicked,
            isFollowed = post.isFollowedByCurrentUser,
            isEnabled = hasAccessToken,
            isVisible = hasAccessToken
        )
    }

    private fun buildTagItems(post: ReaderPost, onClicked: (String) -> Unit) =
        readerPostTagsUiStateBuilder.mapPostTagsToTagUiStates(post, onClicked)

    private fun buildTagItemsVisibility(post: ReaderPost) = post.tags.isNotEmpty()

    private fun buildDateLine(post: ReaderPost) =
        dateTimeUtilsWrapper.javaDateToTimeSpan(post.getDisplayDate(dateTimeUtilsWrapper))

    private fun buildInteractionSection(
        post: ReaderPost,
        onLikesClicked: () -> Unit,
        onCommentsClicked: () -> Unit,
    ) = InteractionSectionUiState(
        likeCount = post.numLikes,
        commentCount = post.numReplies,
        onLikesClicked = onLikesClicked,
        onCommentsClicked = onCommentsClicked,
    )
}
