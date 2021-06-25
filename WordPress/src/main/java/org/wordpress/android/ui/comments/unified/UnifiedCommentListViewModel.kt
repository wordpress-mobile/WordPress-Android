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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.ClickAction
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.Comment
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.SubHeader
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.ToggleAction
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.CommentsListUiModel.WithData
import org.wordpress.android.ui.comments.unified.PagedListLoadingState.Empty
import org.wordpress.android.ui.comments.unified.PagedListLoadingState.EmptyError
import org.wordpress.android.ui.comments.unified.PagedListLoadingState.Error
import org.wordpress.android.ui.comments.unified.PagedListLoadingState.Idle
import org.wordpress.android.ui.comments.unified.PagedListLoadingState.Loading
import org.wordpress.android.ui.comments.unified.PagedListLoadingState.Refreshing
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class UnifiedCommentListViewModel @Inject constructor(
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    // TODO we would like to explore moving PagingSource into the repository
    val commentListItemPager = Pager(PagingConfig(pageSize = 30, initialLoadSize = 30)) {
        CommentPagingSource(
                networkUtilsWrapper
        )
    }

    private val _commentListLoadingState: MutableStateFlow<PagedListLoadingState> = MutableStateFlow(Loading)
    private val _onSnackbarMessage = MutableSharedFlow<SnackbarMessageHolder>()
    private val _selectedIds = MutableStateFlow(emptyList<Long>())
    private val _rawComments: Flow<PagingData<CommentModel>> = commentListItemPager.flow.cachedIn(viewModelScope)

    val onSnackbarMessage: SharedFlow<SnackbarMessageHolder> = _onSnackbarMessage

    val uiState: StateFlow<CommentsUiModel> = combine(_commentListLoadingState) { commentListLoadingState ->
        CommentsUiModel(buildCommentsListUiModel(commentListLoadingState.first()))
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(UI_STATE_FLOW_TIMEOUT_MS),
            initialValue = CommentsUiModel.buildInitialState()
    )

    val commentListData: StateFlow<PagingData<UnifiedCommentListItem>> = combine(
            _rawComments,
            _selectedIds
    ) { commentModels, selectedIds ->
        commentModels.map { commentModel ->
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
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(UI_STATE_FLOW_TIMEOUT_MS),
            initialValue = PagingData.empty()
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

    fun onLoadStateChanged(loadingState: PagedListLoadingState) {
        launch(bgDispatcher) {
            if (loadingState is Error) {
                val errorMessage = loadingState.throwable.message
                if (!errorMessage.isNullOrEmpty()) {
                    _onSnackbarMessage.emit(SnackbarMessageHolder(UiStringText(errorMessage)))
                }
            }
            _commentListLoadingState.value = loadingState
        }
    }

    data class CommentsUiModel(
        val commentsListUiModel: CommentsListUiModel
    ) {
        companion object {
            fun buildInitialState(): CommentsUiModel {
                return CommentsUiModel(
                        commentsListUiModel = CommentsListUiModel.Loading
                )
            }
        }
    }

    sealed class CommentsListUiModel {
        object WithData : CommentsListUiModel()

        data class Empty(
            val title: UiString,
            val image: Int? = null
        ) : CommentsListUiModel()

        object Loading : CommentsListUiModel()
        object Refreshing : CommentsListUiModel()

        object Initial : CommentsListUiModel()
    }

    fun buildCommentsListUiModel(
        commentListLoadingState: PagedListLoadingState
    ): CommentsListUiModel {
        return when (commentListLoadingState) {
            is Loading -> {
                CommentsListUiModel.Loading
            }
            is Refreshing -> {
                CommentsListUiModel.Refreshing
            }
            is Empty -> {
                CommentsListUiModel.Empty(
                        UiStringRes(string.comments_empty_list),
                        drawable.img_illustration_empty_results_216dp
                )
            }
            is EmptyError -> {
                val errorMessage = commentListLoadingState.throwable.localizedMessage
                val errorString = if (errorMessage.isNullOrEmpty()) {
                    UiStringRes(string.error_refresh_comments)
                } else {
                    UiStringText(errorMessage)
                }
                CommentsListUiModel.Empty(
                        errorString,
                        drawable.img_illustration_empty_results_216dp
                )
            }
            is Error,
            is Idle -> {
                WithData
            }
        }
    }

    companion object {
        private const val UI_STATE_FLOW_TIMEOUT_MS = 5000L
    }
}
