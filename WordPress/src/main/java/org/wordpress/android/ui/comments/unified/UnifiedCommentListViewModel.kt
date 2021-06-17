package org.wordpress.android.ui.comments.unified

import androidx.lifecycle.viewModelScope
import androidx.paging.CombinedLoadStates
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadStates
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
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.ClickAction
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.Comment
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.SubHeader
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.ToggleAction
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
//    @OptIn(ExperimentalPagingApi::class)
//    val commentListItemPager = Pager(
//            config = PagingConfig(pageSize = 30, initialLoadSize = 30),
//            remoteMediator = CommentRemoteMediator(networkUtilsWrapper),
//            pagingSourceFactory = {
//                val source =  CommentPagingSource()
//                mediator.addListener { source.invalidate() } // <-- listening and calling invalidate
//                source
//            }
//    )

    @OptIn(ExperimentalPagingApi::class)
    fun createFlow(): Flow<PagingData<CommentModel>> {
        val mediator = CommentRemoteMediator(networkUtilsWrapper)
        val config = PagingConfig(pageSize = 30, initialLoadSize = 30)
        val pager = Pager(
                config = config,
                remoteMediator = mediator,
                pagingSourceFactory = {
                    val source = CommentPagingSource()
                    mediator.addListener { source.invalidate() }
                    source
                }
        )
        return pager.flow.cachedIn(viewModelScope)
    }

    private val _commentListLoadingState = MutableStateFlow(CommentsListLoadingState(idleLoadState, true))
    private val _onSnackbarMessage = MutableSharedFlow<SnackbarMessageHolder>()
    private val _selectedIds = MutableStateFlow(emptyList<Long>())

    val onSnackbarMessage: SharedFlow<SnackbarMessageHolder> = _onSnackbarMessage

    val uiState: StateFlow<CommentsUiModel> = combine(_commentListLoadingState) { commentListLoadingState ->
        CommentsUiModel(buildCommentsListUiModel(commentListLoadingState.first()))
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(UI_STATE_FLOW_TIMEOUT_MS),
            initialValue = CommentsUiModel.buildInitialState()
    )

    private val commentModels: Flow<PagingData<CommentModel>> = createFlow()

    val commentListData: StateFlow<PagingData<UnifiedCommentListItem>> = combine(
            commentModels,
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

    fun onLoadStateChanged(loadState: CombinedLoadStates, isAdapterEmpty: Boolean) {
        launch(bgDispatcher) {
            if (loadState.refresh is LoadState.Error && !isAdapterEmpty) {
                val errorMessage = (loadState.refresh as LoadState.Error).error.message
                if (!errorMessage.isNullOrEmpty()) {
                    _onSnackbarMessage.emit(SnackbarMessageHolder(UiStringText(errorMessage)))
                }
            }
            _commentListLoadingState.emit(CommentsListLoadingState(loadState, isAdapterEmpty))
        }
    }

    data class CommentsUiModel(
        val commentsListUiModel: CommentsListUiModel
    ) {
        companion object {
            fun buildInitialState(): CommentsUiModel {
                return CommentsUiModel(
                        commentsListUiModel = CommentsListUiModel.Empty(UiStringRes(string.comments_fetching), null)
                )
            }
        }
    }

    data class CommentsListLoadingState(
        val loadState: CombinedLoadStates,
        val isAdapterEmpty: Boolean
    )

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
        commentListLoadingState: CommentsListLoadingState
    ): CommentsListUiModel {
        val isAdapterEmpty = commentListLoadingState.isAdapterEmpty
        val loadState = commentListLoadingState.loadState

        val isLoading = loadState.refresh is LoadState.Loading && isAdapterEmpty
        val isRefreshing = loadState.refresh is LoadState.Loading && !isAdapterEmpty
        val isNothingToShow = loadState.refresh is LoadState.NotLoading && loadState.append.endOfPaginationReached && isAdapterEmpty
        val isError = loadState.refresh is LoadState.Error
        val loadingSuccessful = !isLoading && !isNothingToShow && !isError

        when {
            isLoading -> {
                return CommentsListUiModel.Loading
            }
            isRefreshing -> {
                return CommentsListUiModel.Refreshing
            }
            isNothingToShow -> {
                return CommentsListUiModel.Empty(
                        UiStringRes(string.comments_empty_list),
                        R.drawable.img_illustration_empty_results_216dp
                )
            }
            isError -> {
                val errorMessage = (loadState.refresh as LoadState.Error).error.localizedMessage
                val errorString = if (errorMessage.isNullOrEmpty()) {
                    UiStringRes(R.string.error_refresh_comments)
                } else {
                    UiStringText(errorMessage)
                }
                return if (isAdapterEmpty) {
                    CommentsListUiModel.Empty(
                            errorString,
                            R.drawable.img_illustration_empty_results_216dp
                    )
                } else {
                    CommentsListUiModel.WithData
                }
            }
            loadingSuccessful -> {
                return CommentsListUiModel.WithData
            }
        }
        return CommentsListUiModel.Empty(UiStringRes(string.comments_fetching), null)
    }

    companion object {
        private const val UI_STATE_FLOW_TIMEOUT_MS = 5000L
        private val idleLoadState = CombinedLoadStates(
                refresh = LoadState.Loading,
                prepend = NotLoading(endOfPaginationReached = false),
                append = NotLoading(endOfPaginationReached = false),
                source = LoadStates(
                        refresh = LoadState.Loading,
                        prepend = NotLoading(endOfPaginationReached = false),
                        append = NotLoading(endOfPaginationReached = false)
                )
        )
    }
}
