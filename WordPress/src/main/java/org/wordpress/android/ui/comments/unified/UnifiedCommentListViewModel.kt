package org.wordpress.android.ui.comments.unified

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.ClickAction
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.Comment
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.SubHeader
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.ToggleAction
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.CommentsListUiModel.Data
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class UnifiedCommentListViewModel @Inject constructor(
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    // TODO we would like to explore moving PagingSource into the repository
    val commentListItemPager = Pager(PagingConfig(pageSize = 30, initialLoadSize = 30)) { CommentPagingSource() }

    private val _selectedIds = MutableStateFlow(emptyList<Long>())
    val commentListItems: Flow<PagingData<CommentModel>> = commentListItemPager.flow.cachedIn(viewModelScope)

    val uiState: StateFlow<CommentsUiModel> = combine(commentListItems, _selectedIds) { commentListItems, selectedIds ->
        val mappedCommentListItems = commentListItems.map { commentModel ->
            val toggleAction = ToggleAction(commentModel.remoteCommentId, this::toggleItem)
            val clickAction = ClickAction(commentModel.remoteCommentId, this::clickItem)

            val isSelected = selectedIds.contains(commentModel.remoteCommentId)
            val isPending = commentModel.status == UNAPPROVED.toString()

            Comment(
                    remoteCommentId = commentModel.remoteCommentId,
                    postTitle = commentModel.postTitle,
                    authorName = commentModel.authorName,
                    authorEmail = commentModel.authorEmail,
                    content = commentModel.content,
                    publishedDate = commentModel.datePublished,
                    publishedTimestamp = commentModel.publishedTimestamp,
                    authorAvatarUrl = commentModel.authorProfileImageUrl,
                    isPending = isPending,
                    isSelected = isSelected,
                    clickAction = clickAction,
                    toggleAction = toggleAction
            )
        }
                .insertSeparators { before, current ->
                    when {
                        before == null && current != null -> SubHeader(getFormattedDate(current), -1)
                        before != null && current != null && shouldAddSeparator(before, current) -> SubHeader(
                                getFormattedDate(current),
                                -1
                        )
                        else -> null
                    }
                }

        CommentsUiModel(Data(mappedCommentListItems))
    }.stateIn(
            scope = viewModelScope,
            started = Companion.WhileSubscribed(5000),
            initialValue = CommentsUiModel.buildInitialState()
    )

    fun shouldAddSeparator(before: Comment, after: Comment): Boolean {
        return getFormattedDate(before) != getFormattedDate(after)
    }

    private fun getFormattedDate(comment: Comment): String {
        return dateTimeUtilsWrapper.javaDateToTimeSpan(DateTimeUtils.dateFromIso8601(comment.publishedDate))
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

    data class CommentsUiModel(
        val commentsListUiModel: CommentsListUiModel
    ) {
        companion object {
            fun buildInitialState(): CommentsUiModel {
                return CommentsUiModel(
                        commentsListUiModel = CommentsListUiModel.Initial,
                )
            }
        }
    }

    sealed class CommentsListUiModel {
        data class Data(val pagingData: PagingData<UnifiedCommentListItem>) :
                CommentsListUiModel()

        object Initial : CommentsListUiModel()
    }
}
