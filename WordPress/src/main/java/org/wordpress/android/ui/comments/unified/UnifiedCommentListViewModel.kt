package org.wordpress.android.ui.comments.unified

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.models.usecases.ModerateCommentsUseCase.ModerateCommentParameters
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginationResult
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginationResult.PaginationFailure
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginationResult.PaginationLoading
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginationResult.PaginationSuccess
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters
import org.wordpress.android.models.usecases.PropagateCommentsUpdateHandler
import org.wordpress.android.models.usecases.UnifiedCommentsListHandler
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.comments.unified.CommentFilter.PENDING
import org.wordpress.android.ui.comments.unified.CommentFilter.SPAM
import org.wordpress.android.ui.comments.unified.CommentFilter.TRASHED
import org.wordpress.android.ui.comments.unified.CommentFilter.UNREPLIED
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.ClickAction
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.Comment
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.NextPageLoader
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.SubHeader
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.ToggleAction
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.ActionModeUiModel.Hidden
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.ActionModeUiModel.Visible
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.CommentsListUiModel.WithData
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
    private val selectedSiteRepository: SelectedSiteRepository,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val unifiedCommentsListHandler: UnifiedCommentsListHandler,
    private val propagateCommentsUpdateHandler: PropagateCommentsUpdateHandler
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false
    private lateinit var commentFilter: CommentFilter

    private val _commentsProvider = unifiedCommentsListHandler.subscribe()
    private val _commentsUpdateListener = propagateCommentsUpdateHandler.subscribe()

    private val _onSnackbarMessage = MutableSharedFlow<SnackbarMessageHolder>()
    private val _selectedComments = MutableStateFlow(emptyList<SelectedComment>())

    val onSnackbarMessage: SharedFlow<SnackbarMessageHolder> = _onSnackbarMessage

    val uiState: StateFlow<CommentsUiModel> by lazy {
        combine(
                _commentsProvider,
                _selectedComments
        ) { commentData, selectedIds ->
//            if (moderationEvents is ModerationSuccess || moderationEvents is ModerationFailure) {
//                propagateCommentsUpdateHandler.requestCommentsUpdate()
//            }

            CommentsUiModel(
                    buildCommentList(commentData, selectedIds),
                    buildCommentsListUiModel(commentData),
                    buildActionModeUiModel(selectedIds, commentFilter)
            )
        }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Companion.WhileSubscribed(UI_STATE_FLOW_TIMEOUT_MS),
                initialValue = CommentsUiModel.buildInitialState()
        )
    }

    fun setup(commentListFilter: CommentFilter) {
        if (isStarted) return
        isStarted = true

        commentFilter = commentListFilter

        listToLocalCacheUpdateRequests()
        listenToSnackBarRequests()
        requestsFirstPage()
    }

    private fun requestsFirstPage() {
        launch(bgDispatcher) {
            unifiedCommentsListHandler.requestPage(
                    Parameters(
                            site = selectedSiteRepository.getSelectedSite()!!,
                            number = if (commentFilter == UNREPLIED) 100 else 30,
                            offset = 0,
                            commentFilter = commentFilter
                    )
            )
        }
    }

    private fun listToLocalCacheUpdateRequests() {
        launch(bgDispatcher) {
            _commentsUpdateListener.collectLatest {
                unifiedCommentsListHandler.refreshFromCache(
                        Parameters(
                                site = selectedSiteRepository.getSelectedSite()!!,
                                number = if (commentFilter == UNREPLIED) 100 else 30,
                                offset = 0,
                                commentFilter = commentFilter
                        )
                )
            }
        }
    }

    private fun listenToSnackBarRequests() {
        launch(bgDispatcher) {
            _commentsProvider.filter { it is PaginationFailure }.collectLatest {
                val errorMessage = (it as PaginationFailure).error.message
                if (!errorMessage.isNullOrEmpty()) {
                    _onSnackbarMessage.emit(SnackbarMessageHolder(UiStringText(errorMessage)))
                }
            }
        }
    }

    fun reload() {
        requestsFirstPage()
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
            // open comment details
        }
    }

    fun clearSelection() {
        viewModelScope.launch {
            if (!_selectedComments.value.isNullOrEmpty()) {
                _selectedComments.emit(listOf())
            }
        }
    }

    fun performBatchModeration(newStatus: CommentStatus) {
        launch(bgDispatcher) {
            unifiedCommentsListHandler.moderateComments(
                    ModerateCommentParameters(
                            selectedSiteRepository.getSelectedSite()!!,
                            _selectedComments.value.map { it.remoteCommentId },
                            APPROVED
                    )
            )
        }
    }

    private fun shouldAddSeparator(before: CommentEntity, after: CommentEntity): Boolean {
        return getFormattedDate(before) != getFormattedDate(after)
    }

    private fun getFormattedDate(comment: CommentEntity): String {
        return dateTimeUtilsWrapper.javaDateToTimeSpan(DateTimeUtils.dateFromIso8601(comment.datePublished))
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

    private fun buildCommentList(
        commentsDataResult: PaginationResult,
        selectedComments: List<SelectedComment>?
    ): List<UnifiedCommentListItem> {
        var (comments, hasMore) = when (commentsDataResult) {
            is PaginationFailure -> Pair(
                    commentsDataResult.cachedData.comments,
                    commentsDataResult.cachedData.hasMore
            )
            PaginationLoading -> Pair(listOf(), false)
            is PaginationSuccess -> Pair(commentsDataResult.data.comments, commentsDataResult.data.hasMore)
        }

        val list = ArrayList<UnifiedCommentListItem>()
        comments.forEachIndexed { index, commentModel ->
            val previousItem = comments.getOrNull(index - 1)
            if (previousItem == null) {
                list.add(SubHeader(getFormattedDate(commentModel), -1))
            } else if (shouldAddSeparator(previousItem, commentModel)) {
                list.add(SubHeader(getFormattedDate(commentModel), -1))
            }

            val toggleAction = ToggleAction(
                    commentModel.remoteCommentId,
                    CommentStatus.fromString(commentModel.status), this::toggleItem
            )
            val clickAction = ClickAction(
                    commentModel.remoteCommentId,
                    CommentStatus.fromString(commentModel.status),
                    this::clickItem
            )

            val isSelected = selectedComments?.any { it.remoteCommentId == commentModel.remoteCommentId }
            val isPending = commentModel.status == UNAPPROVED.toString()

            list.add(
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
                            isSelected = isSelected ?: false,
                            clickAction = clickAction,
                            toggleAction = toggleAction
                    )
            )
        }

        if (hasMore && commentFilter != UNREPLIED) {
            list.add(NextPageLoader(hasMore && commentsDataResult !is PaginationFailure, -1) {
                launch(bgDispatcher) {
                    unifiedCommentsListHandler.requestPage(
                            Parameters(
                                    site = selectedSiteRepository.getSelectedSite()!!,
                                    number = 30,
                                    offset = comments.size,
                                    commentFilter = commentFilter
                            )
                    )
                }
            })
        }
        return list
    }

    private fun buildCommentsListUiModel(
        commentsDataResult: PaginationResult,
    ): CommentsListUiModel {
        return when (commentsDataResult) {
            is PaginationLoading -> {
                val prevState = uiState.replayCache.firstOrNull()
                if (prevState != null && prevState.commentData.isNotEmpty()) {
                    CommentsListUiModel.Refreshing
                } else {
                    CommentsListUiModel.Loading
                }
            }
            is PaginationSuccess -> {
                if ( commentsDataResult.data.comments.isEmpty()) {
                    CommentsListUiModel.Empty(
                            UiStringRes(string.comments_empty_list),
                            R.drawable.img_illustration_empty_results_216dp
                    )
                } else {
                    WithData
                }
            }
            is PaginationFailure -> {
                val errorMessage = commentsDataResult.error.message
                if (commentsDataResult.cachedData.comments.isEmpty()) {
                    val errorString = if (errorMessage.isNullOrEmpty()) {
                        UiStringRes(string.error_refresh_comments)
                    } else {
                        UiStringText(errorMessage)
                    }
                    CommentsListUiModel.Empty(
                            errorString,
                            R.drawable.img_illustration_empty_results_216dp
                    )
                } else {
                    return WithData
                }
            }
        }
    }

    data class CommentsUiModel(
        val commentData: List<UnifiedCommentListItem>,
        val commentsListUiModel: CommentsListUiModel,
        val actionModeUiModel: ActionModeUiModel
    ) {
        companion object {
            fun buildInitialState(): CommentsUiModel {
                return CommentsUiModel(
                        commentData = emptyList(),
                        commentsListUiModel = CommentsListUiModel.Loading,
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
