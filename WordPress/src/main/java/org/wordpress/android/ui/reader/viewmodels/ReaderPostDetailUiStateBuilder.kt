package org.wordpress.android.ui.reader.viewmodels

import dagger.Reusable
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.discover.ReaderPostActions
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.SecondaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.discover.ReaderPostUiStateBuilder
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.CommentsEntryUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.ReaderPostDetailsUiState
import org.wordpress.android.ui.reader.views.ReaderPostDetailsHeaderViewUiStateBuilder
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderViewUiState.ReaderPostDetailsHeaderUiState
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

@Reusable
class ReaderPostDetailUiStateBuilder @Inject constructor(
    private val postDetailsHeaderViewUiStateBuilder: ReaderPostDetailsHeaderViewUiStateBuilder,
    private val postUiStateBuilder: ReaderPostUiStateBuilder,
    private val accountStore: AccountStore
) {
    fun mapPostToUiState(
        post: ReaderPost,
        moreMenuItems: List<SecondaryAction>? = null,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit,
        onBlogSectionClicked: (Long, Long) -> Unit,
        onFollowClicked: () -> Unit,
        onTagItemClicked: (String) -> Unit
    ): ReaderPostDetailsUiState {
        return ReaderPostDetailsUiState(
                postId = post.postId,
                blogId = post.blogId,
                headerUiState = buildPostDetailsHeaderUiState(
                        post,
                        onBlogSectionClicked,
                        onFollowClicked,
                        onTagItemClicked
                ),
                moreMenuItems = moreMenuItems,
                actions = buildPostActions(post, onButtonClicked),
                commentsEntrySection = buildCommentsEntryUiState(post, onButtonClicked)
        )
    }

    private fun buildPostDetailsHeaderUiState(
        post: ReaderPost,
        onBlogSectionClicked: (Long, Long) -> Unit,
        onFollowClicked: () -> Unit,
        onTagItemClicked: (String) -> Unit
    ): ReaderPostDetailsHeaderUiState {
        return postDetailsHeaderViewUiStateBuilder.mapPostToUiState(
                post,
                onBlogSectionClicked,
                onFollowClicked,
                onTagItemClicked
        )
    }

    private fun buildPostActions(
        post: ReaderPost,
        onButtonClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ): ReaderPostActions {
        return postUiStateBuilder.mapPostToActions(post, onButtonClicked)
    }

    private fun buildCommentsEntryUiState(
        post: ReaderPost,
        onAddCommentsClicked: (Long, Long, ReaderPostCardActionType) -> Unit
    ): CommentsEntryUiState {
        val showCommentsEntry = when {
            post.isDiscoverPost -> false
            !accountStore.hasAccessToken() -> post.numReplies > 0
            else -> post.isWP && (post.isCommentsOpen || post.numReplies > 0)
        }

        val labelText = UiStringResWithParams(
                string.reader_no_of_comments,
                listOf(UiStringText(post.numReplies.toString()))
        )
        val actionText = UiStringRes(string.reader_add_comments)

        return CommentsEntryUiState(
                isVisible = showCommentsEntry,
                labelText = labelText,
                actionText = actionText,
                onClicked = onAddCommentsClicked
        )
    }
}
