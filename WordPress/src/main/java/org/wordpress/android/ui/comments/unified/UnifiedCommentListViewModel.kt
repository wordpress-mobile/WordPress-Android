package org.wordpress.android.ui.comments.unified

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.ClickAction
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.Comment
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.SubHeader
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.ToggleAction
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class UnifiedCommentListViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    // TODO we woiuld like to explore moving PagingSource into the repository
    val commentListItemPager = Pager(PagingConfig(pageSize = 30, initialLoadSize = 30)) { CommentPagingSource() }

    val commentListItems = commentListItemPager.flow
            .map { pagingData ->
                pagingData.map { commentModel ->
                    val toggleAction = ToggleAction(commentModel.remoteCommentId, this::clickItem)
                    val clickAction = ClickAction(commentModel.remoteCommentId, this::toggleItem)
                    Comment(
                            remoteCommentId = commentModel.remoteCommentId,
                            postTitle = commentModel.postTitle,
                            authorName = commentModel.authorName,
                            authorEmail = commentModel.authorEmail,
                            body = commentModel.content,
                            avatarUrl = "",
                            isPending = commentModel.status == CommentStatus.UNAPPROVED.toString(),
                            isSelected = false,
                            clickAction = clickAction,
                            toggleAction = toggleAction
                    )
                }
                        .insertSeparators { before, after ->
                            when {
                                before == null -> SubHeader("Date Sub Header", -1)
                                // TODO add the logic for determining if subheader is necessary or not
                                else -> null
                            }
                        }
            }

    fun start() {
        if (isStarted) return
        isStarted = true
    }

    fun toggleItem(remoteCommentId: Long) {
        // TODO toggle comment selection for batch moderation
    }

    fun clickItem(remoteCommentId: Long) {
        // TODO open comment details
    }
}
