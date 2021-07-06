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
import kotlinx.coroutines.launch
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.store.UnifiedCommentStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.comments.unified.CommentFilter.PENDING
import org.wordpress.android.ui.comments.unified.CommentFilter.SPAM
import org.wordpress.android.ui.comments.unified.CommentFilter.TRASHED
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
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.ActionModeUiModel.Hidden
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.ActionModeUiModel.Visible
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class UnifiedCommentListViewModel @Inject constructor(
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val resourceProvider: ResourceProvider,
    private val commentStore: UnifiedCommentStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false
    private lateinit var commentFilter: CommentFilter

    var pagingSource: CommentPagingSource? = null

    // TODO we would like to explore moving PagingSource into the repository
    val commentListItemPager = Pager(PagingConfig(pageSize = 30, initialLoadSize = 30)) {
        pagingSource = CommentPagingSource(
                commentFilter,
                networkUtilsWrapper,
                commentStore,
                selectedSiteRepository.getSelectedSite()
        )
        pagingSource!!
    }

    private val _commentListLoadingState: MutableStateFlow<PagedListLoadingState> = MutableStateFlow(Loading)
    private val _onSnackbarMessage = MutableSharedFlow<SnackbarMessageHolder>()
    private val _selectedComments = MutableStateFlow(emptyList<SelectedComment>())
    private val _rawComments: Flow<PagingData<CommentModel>> = commentListItemPager.flow.cachedIn(viewModelScope)

    val onSnackbarMessage: SharedFlow<SnackbarMessageHolder> = _onSnackbarMessage

    val commentListData: StateFlow<PagingData<UnifiedCommentListItem>> = combine(
            _rawComments,
            _selectedComments
    ) { commentModels, selectedComments ->
        commentModels.map { commentModel ->
            val toggleAction = ToggleAction(
                    commentModel.remoteCommentId,
                    CommentStatus.fromString(commentModel.status), this::toggleItem
            )
            val clickAction = ClickAction(
                    commentModel.remoteCommentId,
                    CommentStatus.fromString(commentModel.status),
                    this::clickItem
            )

            val isSelected = selectedComments.any { it.remoteCommentId == commentModel.remoteCommentId }
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
    }.cachedIn(viewModelScope).stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(UI_STATE_FLOW_TIMEOUT_MS),
            initialValue = PagingData.empty()
    )

    val uiState: StateFlow<CommentsUiModel> = combine(
            _commentListLoadingState,
            _selectedComments
    ) { commentListLoadingState, selectedIds ->
        CommentsUiModel(
                buildCommentsListUiModel(commentListLoadingState),
                buildActionModeUiModel(selectedIds, commentFilter)
        )
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(UI_STATE_FLOW_TIMEOUT_MS),
            initialValue = CommentsUiModel.buildInitialState()
    )

    fun setup(commentListFilter: CommentFilter) {
        if (isStarted) return
        isStarted = true

        commentFilter = commentListFilter
    }

    private fun toggleItem(remoteCommentId: Long, commentStatus: CommentStatus) {
        viewModelScope.launch {
            val selectedComment = SelectedComment(remoteCommentId, commentStatus)
            val selectedComments = _selectedComments.value.toMutableList()
            if (selectedComments.contains(selectedComment)) {
                selectedComments.remove(selectedComment)
            } else {
                selectedComments.add(selectedComment)
            }
            _selectedComments.emit(selectedComments)
        }
    }

    private fun clickItem(remoteCommentId: Long, commentStatus: CommentStatus) {
        if (_selectedComments.value.isNotEmpty()) {
            toggleItem(remoteCommentId, commentStatus)
        } else {
            launch(bgDispatcher) {
                commentStore.moderateComment(selectedSiteRepository.getSelectedSite()!!, remoteCommentId, UNAPPROVED)
                pagingSource?.invalidate()
                commentStore.pushComment(selectedSiteRepository.getSelectedSite()!!, remoteCommentId)
                pagingSource?.invalidate()
            }
        }
    }

    fun clearSelection() {
        viewModelScope.launch {
            if (!_selectedComments.value.isNullOrEmpty()) {
                _selectedComments.emit(listOf())
            }
        }
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

    fun performBatchApprove() {
        // TODO batch approve
    }

    private fun shouldAddSeparator(before: Comment, after: Comment): Boolean {
        return getFormattedDate(before) != getFormattedDate(after)
    }

    private fun getFormattedDate(comment: Comment): String {
        return dateTimeUtilsWrapper.javaDateToTimeSpan(DateTimeUtils.dateFromIso8601(comment.publishedDate))
    }

    private fun buildActionModeUiModel(
        selectedComments: List<SelectedComment>?,
        commentListFilter: CommentFilter
    ): ActionModeUiModel {
        if (selectedComments.isNullOrEmpty()) {
            return Hidden
        }
        val title: UiString? = when (val numSelected = selectedComments.size) {
            0 -> null
            else -> {
                UiStringText(String.format(resourceProvider.getString(string.cab_selected), numSelected))
            }
        }

        val approveActionUiModel = ActionUiModel(true, selectedComments.any { it.status == UNAPPROVED })
        val unaproveActionUiModel = ActionUiModel(
                true,
                (commentListFilter != TRASHED && commentListFilter != SPAM && commentListFilter != PENDING) && selectedComments.any { it.status == APPROVED }
        )
        val spamActionUiModel = ActionUiModel(commentListFilter != SPAM, true)
        val unspamActionUiModel = ActionUiModel(commentListFilter == SPAM, true)
        val trashActionUiModel = ActionUiModel(commentListFilter != TRASHED, true)
        val unTrashActionUiModel = ActionUiModel(commentListFilter == TRASHED, true)
        val deleteActionUiModel = ActionUiModel(commentListFilter == TRASHED, true)

        return Visible(
                title,
                approveActionUiModel,
                unaproveActionUiModel,
                spamActionUiModel,
                unspamActionUiModel,
                trashActionUiModel,
                unTrashActionUiModel,
                deleteActionUiModel
        )
    }

    private fun buildCommentsListUiModel(
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

    data class CommentsUiModel(
        val commentsListUiModel: CommentsListUiModel,
        val actionModeUiModel: ActionModeUiModel
    ) {
        companion object {
            fun buildInitialState(): CommentsUiModel {
                return CommentsUiModel(
                        commentsListUiModel = CommentsListUiModel.Initial,
                        actionModeUiModel = Hidden
                )
            }
        }
    }

    sealed class ActionModeUiModel {
        data class Visible(
            val actionModeTitle: UiString? = null,
            val approveActionUiModel: ActionUiModel,
            val unparoveActionUiModel: ActionUiModel,
            val spamActionUiModel: ActionUiModel,
            val unspamActionUiModel: ActionUiModel,
            val trashActionUiModel: ActionUiModel,
            val unTrashActionUiModel: ActionUiModel,
            val deleteActionUiModel: ActionUiModel
        ) : ActionModeUiModel()

        object Hidden : ActionModeUiModel()
    }

    data class ActionUiModel(
        val isVisible: Boolean = false,
        val isEnabled: Boolean = false
    )

    data class SelectedComment(val remoteCommentId: Long, val status: CommentStatus)

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

    companion object {
        private const val UI_STATE_FLOW_TIMEOUT_MS = 5000L
    }
}
