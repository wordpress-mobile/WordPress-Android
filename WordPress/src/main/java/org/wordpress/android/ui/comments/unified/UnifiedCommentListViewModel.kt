package org.wordpress.android.ui.comments.unified

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.comments.CommentsStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.comments.unified.CommentFilter.PENDING
import org.wordpress.android.ui.comments.unified.CommentFilter.SPAM
import org.wordpress.android.ui.comments.unified.CommentFilter.TRASHED
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.ClickAction
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.Comment
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.NextPageLoader
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.SubHeader
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.ToggleAction
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.ActionModeUiModel.Hidden
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.ActionModeUiModel.Visible
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString
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
    private val commentsStore: CommentsStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false
    private lateinit var commentFilter: CommentFilter

    private val _rawComments: SharedFlow<List<CommentEntity>> by lazy {
        commentsStore.getCommentsFlow(
                selectedSiteRepository.getSelectedSite()!!.siteId,
                commentFilter.toCommentStatuses()
        ).shareIn(
                scope = viewModelScope,
                started = SharingStarted.Companion.WhileSubscribed(UI_STATE_FLOW_TIMEOUT_MS)
        )
    }

    private val _onSnackbarMessage = MutableSharedFlow<SnackbarMessageHolder>()
    private val _selectedComments = MutableStateFlow(emptyList<SelectedComment>())

    val onSnackbarMessage: SharedFlow<SnackbarMessageHolder> = _onSnackbarMessage

    val uiState: StateFlow<CommentsUiModel> by lazy {
        combine(
                _rawComments,
                _selectedComments
        ) { comments, selectedIds ->
            CommentsUiModel(
                    buildCommentList(comments, selectedIds, comments.size >= 30),
//                buildCommentsListUiModel(commentListLoadingState),
                    CommentsListUiModel.WithData,
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

        launch {
            commentsStore.fetchComments(
                    selectedSiteRepository.getSelectedSite()!!,
                    30,
                    0,
                    commentFilter.toCommentStatus(),
                    commentFilter.toCommentStatuses()
            )
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

    private fun clickItem(remoteCommentId: Long, commentStatus: CommentStatus) {
        if (_selectedComments.value.isNotEmpty()) {
            toggleItem(remoteCommentId, commentStatus)
        } else {
            launch(bgDispatcher) {
                commentsStore.moderateCommentLocally(
                        selectedSiteRepository.getSelectedSite()!!,
                        remoteCommentId,
                        UNAPPROVED
                )
                commentsStore.pushComment(selectedSiteRepository.getSelectedSite()!!, remoteCommentId)
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

//    fun onLoadStateChanged(loadingState: PagedListLoadingState) {
//        launch(bgDispatcher) {
//            if (loadingState is Error) {
//                val errorMessage = loadingState.throwable.message
//                if (!errorMessage.isNullOrEmpty()) {
//                    _onSnackbarMessage.emit(SnackbarMessageHolder(UiStringText(errorMessage)))
//                }
//            }
//            _commentListLoadingState.value = loadingState
//        }
//    }

    fun performBatchApprove() {
        // TODO batch approve
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
        commentEntities: List<CommentEntity>,
        selectedComments: List<SelectedComment>?,
        canLoadMore: Boolean,
    ): List<UnifiedCommentListItem> {
        val list = ArrayList<UnifiedCommentListItem>()
        commentEntities.forEachIndexed { index, commentModel ->
            val previousItem = commentEntities.getOrNull(index - 1)
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

        if (canLoadMore) {
            list.add(NextPageLoader(true, -1) {
                launch {
                    commentsStore.fetchComments(
                            selectedSiteRepository.getSelectedSite()!!,
                            30,
                            commentEntities.size,
                            commentFilter.toCommentStatus(),
                            commentFilter.toCommentStatuses()
                    )
                }
            })
        }
        return list
    }

//    private fun buildCommentsListUiModel(
//
//    ): CommentsListUiModel {
//        return when (commentListLoadingState) {
//            is Loading -> {
//                CommentsListUiModel.Loading
//            }
//            is Refreshing -> {
//                CommentsListUiModel.Refreshing
//            }
//            is Empty -> {
//                CommentsListUiModel.Empty(
//                        UiStringRes(string.comments_empty_list),
//                        drawable.img_illustration_empty_results_216dp
//                )
//            }
//            is EmptyError -> {
//                val errorMessage = commentListLoadingState.throwable.localizedMessage
//                val errorString = if (errorMessage.isNullOrEmpty()) {
//                    UiStringRes(string.error_refresh_comments)
//                } else {
//                    UiStringText(errorMessage)
//                }
//                CommentsListUiModel.Empty(
//                        errorString,
//                        drawable.img_illustration_empty_results_216dp
//                )
//            }
//            is Error,
//            is Idle -> {
//                WithData
//            }
//        }
//    }

    fun refresh() {
        launch {
            commentsStore.fetchComments(
                    selectedSiteRepository.getSelectedSite()!!,
                    30,
                    0,
                    commentFilter.toCommentStatus(),
                    commentFilter.toCommentStatuses()
            )
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
