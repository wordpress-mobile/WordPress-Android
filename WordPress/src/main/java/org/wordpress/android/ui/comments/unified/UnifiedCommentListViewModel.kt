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
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat.COMMENT_BATCH_APPROVED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.COMMENT_BATCH_DELETED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.COMMENT_BATCH_SPAMMED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.COMMENT_BATCH_TRASHED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.COMMENT_BATCH_UNAPPROVED
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.DELETED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.PagingData
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase.Parameters.ModerateCommentsParameters
import org.wordpress.android.models.usecases.CommentsUseCaseType
import org.wordpress.android.models.usecases.CommentsUseCaseType.BATCH_MODERATE_USE_CASE
import org.wordpress.android.models.usecases.CommentsUseCaseType.MODERATE_USE_CASE
import org.wordpress.android.models.usecases.CommentsUseCaseType.PAGINATE_USE_CASE
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateHandler
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.Parameters.ModerateCommentParameters
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.Parameters.ModerateWithFallbackParameters
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase.SingleCommentModerationResult
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters.GetPageParameters
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters.ReloadFromCacheParameters
import org.wordpress.android.models.usecases.UnifiedCommentsListHandler
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.comments.unified.CommentFilter.UNREPLIED
import org.wordpress.android.ui.comments.unified.CommentListUiModelHelper.BatchModerationStatus
import org.wordpress.android.ui.comments.unified.CommentListUiModelHelper.CommentsUiModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.usecase.UseCaseResult
import org.wordpress.android.usecase.UseCaseResult.Failure
import org.wordpress.android.usecase.UseCaseResult.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

typealias CommentsPagingResult = UseCaseResult<CommentsUseCaseType, CommentError, PagingData>

class UnifiedCommentListViewModel @Inject constructor(
    private val commentListUiModelHelper: CommentListUiModelHelper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val unifiedCommentsListHandler: UnifiedCommentsListHandler,
    localCommentCacheUpdateHandler: LocalCommentCacheUpdateHandler
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false
    private lateinit var commentFilter: CommentFilter

    private val _commentsUpdateListener = localCommentCacheUpdateHandler.subscribe()

    private val _onSnackbarMessage = MutableSharedFlow<SnackbarMessageHolder>()
    private val _selectedComments = MutableStateFlow(emptyList<SelectedComment>())

    private val _batchModerationStatus = MutableStateFlow<BatchModerationStatus>(BatchModerationStatus.Idle)

    val onSnackbarMessage: SharedFlow<SnackbarMessageHolder> = _onSnackbarMessage

    private var commentInModeration = ArrayList<Long>()

    // TODO maybe we can change to some generic Action pattern
    private val _onCommentDetailsRequested = MutableSharedFlow<SelectedComment>()
    val onCommentDetailsRequested: SharedFlow<SelectedComment> = _onCommentDetailsRequested

    private val _commentsProvider = unifiedCommentsListHandler.subscribe()

    val uiState: StateFlow<CommentsUiModel> by lazy {
        combine(
            _commentsProvider.filter { it.type == PAGINATE_USE_CASE }.filterIsInstance<CommentsPagingResult>(),
            _selectedComments,
            _batchModerationStatus
        ) { commentData, selectedCommentIds, batchModerationStatus ->
            commentListUiModelHelper.buildUiModel(
                commentFilter,
                commentData,
                selectedCommentIds,
                batchModerationStatus,
                uiState.replayCache.firstOrNull(),
                this::toggleItem,
                this::clickItem,
                this::requestNextPage,
                this::moderateSelectedComments,
                this::onBatchModerationConfirmationCanceled
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(UI_STATE_FLOW_TIMEOUT_MS),
            initialValue = CommentsUiModel.buildInitialState()
        )
    }

    fun start(commentListFilter: CommentFilter) {
        if (isStarted) return
        isStarted = true

        commentFilter = commentListFilter

        listenToLocalCacheUpdateRequests()
        listenToSnackBarRequests()
        requestsFirstPage()
    }

    private fun requestsFirstPage() {
        launch(bgDispatcher) {
            unifiedCommentsListHandler.requestPage(
                GetPageParameters(
                    site = selectedSiteRepository.getSelectedSite()!!,
                    number = if (commentFilter == UNREPLIED) UNREPLIED_COMMENT_PAGE_SIZE else COMMENT_PAGE_SIZE,
                    offset = 0,
                    commentFilter = commentFilter
                )
            )
        }
    }

    private fun requestNextPage(offset: Int) {
        launch(bgDispatcher) {
            unifiedCommentsListHandler.requestPage(
                GetPageParameters(
                    site = selectedSiteRepository.getSelectedSite()!!,
                    number = if (commentFilter == UNREPLIED) {
                        UNREPLIED_COMMENT_PAGE_SIZE
                    } else {
                        COMMENT_PAGE_SIZE
                    },
                    offset = offset,
                    commentFilter = commentFilter
                )
            )
        }
    }

    private fun listenToLocalCacheUpdateRequests() {
        launch(bgDispatcher) {
            _commentsUpdateListener.collectLatest {
                launch(bgDispatcher) {
                    unifiedCommentsListHandler.refreshFromCache(
                        ReloadFromCacheParameters(
                            pagingParameters = GetPageParameters(
                                site = selectedSiteRepository.getSelectedSite()!!,
                                number = if (commentFilter == UNREPLIED) {
                                    UNREPLIED_COMMENT_PAGE_SIZE
                                } else {
                                    COMMENT_PAGE_SIZE
                                },
                                offset = 0,
                                commentFilter = commentFilter
                            ),
                            hasMore = uiState.value.commentData.hasMore
                        )
                    )
                }
            }
        }
    }

    private fun listenToSnackBarRequests() {
        launch(bgDispatcher) {
            _commentsProvider.filter { it is Failure }.collectLatest {
                val errorMessage = if (it.type == BATCH_MODERATE_USE_CASE) {
                    UiStringRes(string.comment_batch_moderation_error)
                } else {
                    if ((it as Failure).error.message.isNullOrEmpty()) {
                        null
                    } else {
                        UiStringText(it.error.message)
                    }
                }

                if (errorMessage != null) {
                    _onSnackbarMessage.emit(SnackbarMessageHolder(errorMessage))
                }
            }
        }
        listenToModerateWithUndoSnackbarRequets()
    }

    fun listenToModerateWithUndoSnackbarRequets() {
        launch(bgDispatcher) {
            _commentsProvider.filter { it is Success && it.type == MODERATE_USE_CASE }.collectLatest {
                if (it is Success && it.data is SingleCommentModerationResult) {
                    val message = when (it.data.newStatus) {
                        TRASH -> UiStringRes(string.comment_trashed)
                        SPAM -> UiStringRes(string.comment_spammed)
                        else -> UiStringRes(string.comment_deleted_permanently)
                    }
                    commentInModeration.add(it.data.remoteCommentId)
                    _onSnackbarMessage.emit(
                        SnackbarMessageHolder(
                            message = message,
                            buttonTitle = UiStringRes(string.undo),
                            buttonAction = {
                                launch(bgDispatcher) {
                                    commentInModeration.remove(it.data.remoteCommentId)
                                    unifiedCommentsListHandler.undoCommentModeration(
                                        ModerateWithFallbackParameters(
                                            selectedSiteRepository.getSelectedSite()!!,
                                            it.data.remoteCommentId,
                                            it.data.newStatus,
                                            it.data.oldStatus
                                        )
                                    )
                                }
                            },
                            onDismissAction = { _ ->
                                launch(bgDispatcher) {
                                    if (commentInModeration.contains(it.data.remoteCommentId)) {
                                        commentInModeration.remove(it.data.remoteCommentId)
                                        unifiedCommentsListHandler.moderateAfterUndo(
                                            ModerateWithFallbackParameters(
                                                selectedSiteRepository.getSelectedSite()!!,
                                                it.data.remoteCommentId,
                                                it.data.newStatus,
                                                it.data.oldStatus
                                            )
                                        )
                                    }
                                }
                            }
                        )
                    )
                }
            }
        }
    }

    fun reload() {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            launch(bgDispatcher) {
                _onSnackbarMessage.emit(SnackbarMessageHolder(UiStringRes(string.no_network_message)))
            }
        } else {
            requestsFirstPage()
        }
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

    private fun clickItem(comment: CommentEntity) {
        if (_selectedComments.value.isNotEmpty()) {
            toggleItem(comment.remoteCommentId, CommentStatus.fromString(comment.status))
        } else {
            launch {
                _onCommentDetailsRequested.emit(
                    SelectedComment(
                        comment.remoteCommentId,
                        CommentStatus.fromString(comment.status)
                    )
                )
            }
        }
    }

    fun clearActionModeSelection() {
        viewModelScope.launch {
            if (!_selectedComments.value.isNullOrEmpty()) {
                _selectedComments.emit(listOf())
            }
        }
    }

    fun performSingleCommentModeration(commentId: Long, newStatus: CommentStatus) {
        launch(bgDispatcher) {
            if (newStatus == SPAM || newStatus == TRASH || newStatus == DELETED) {
                unifiedCommentsListHandler.preModerateWithUndo(
                    ModerateCommentParameters(
                        selectedSiteRepository.getSelectedSite()!!,
                        commentId,
                        newStatus
                    )
                )
            } else {
                launch(bgDispatcher) {
                    unifiedCommentsListHandler.moderateComments(
                        ModerateCommentsParameters(
                            selectedSiteRepository.getSelectedSite()!!,
                            listOf(commentId),
                            newStatus
                        )
                    )
                }
            }
        }
    }

    fun onBatchModerationConfirmationCanceled() {
        launch(bgDispatcher) {
            _batchModerationStatus.emit(BatchModerationStatus.Idle)
        }
    }

    fun performBatchModeration(newStatus: CommentStatus) {
        launch(bgDispatcher) {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                launch(bgDispatcher) {
                    _onSnackbarMessage.emit(SnackbarMessageHolder(UiStringRes(string.no_network_message)))
                }
                return@launch
            }
            if (newStatus == DELETED || newStatus == TRASH) {
                _batchModerationStatus.emit(BatchModerationStatus.AskingToModerate(newStatus))
            } else {
                moderateSelectedComments(newStatus)
            }
        }
    }

    private fun moderateSelectedComments(newStatus: CommentStatus) {
        launch(bgDispatcher) {
            _batchModerationStatus.emit(BatchModerationStatus.Idle)
            val commentsToModerate = _selectedComments.value.map { it.remoteCommentId }
            _selectedComments.emit(emptyList())
            unifiedCommentsListHandler.moderateComments(
                ModerateCommentsParameters(
                    selectedSiteRepository.getSelectedSite()!!,
                    commentsToModerate,
                    newStatus
                )
            )
            when (newStatus) {
                APPROVED -> analyticsTrackerWrapper.track(COMMENT_BATCH_APPROVED)
                UNAPPROVED -> analyticsTrackerWrapper.track(COMMENT_BATCH_UNAPPROVED)
                SPAM -> analyticsTrackerWrapper.track(COMMENT_BATCH_SPAMMED)
                TRASH -> analyticsTrackerWrapper.track(COMMENT_BATCH_TRASHED)
                DELETED -> analyticsTrackerWrapper.track(COMMENT_BATCH_DELETED)
                else -> { // noop
                }
            }
        }
    }

    data class SelectedComment(val remoteCommentId: Long, val status: CommentStatus)

    companion object {
        private const val UI_STATE_FLOW_TIMEOUT_MS = 5000L
        private const val COMMENT_PAGE_SIZE = 30
        private const val UNREPLIED_COMMENT_PAGE_SIZE = 100
    }
}
