package org.wordpress.android.ui.comments.unified

import okhttp3.internal.toImmutableList
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.DELETED
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.ui.comments.unified.CommentFilter.PENDING
import org.wordpress.android.ui.comments.unified.CommentFilter.SPAM
import org.wordpress.android.ui.comments.unified.CommentFilter.TRASHED
import org.wordpress.android.ui.comments.unified.CommentFilter.UNREPLIED
import org.wordpress.android.ui.comments.unified.CommentListUiModelHelper.ActionModeUiModel.Hidden
import org.wordpress.android.ui.comments.unified.CommentListUiModelHelper.ActionModeUiModel.Visible
import org.wordpress.android.ui.comments.unified.CommentListUiModelHelper.BatchModerationStatus.AskingToModerate
import org.wordpress.android.ui.comments.unified.CommentListUiModelHelper.CommentsListUiModel.WithData
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.ClickAction
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.Comment
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.NextPageLoader
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.SubHeader
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.ToggleAction
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.SelectedComment
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.usecase.UseCaseResult.Failure
import org.wordpress.android.usecase.UseCaseResult.Loading
import org.wordpress.android.usecase.UseCaseResult.Success
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class CommentListUiModelHelper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) {
    @Suppress("LongParameterList")
    internal fun buildUiModel(
        commentFilter: CommentFilter,
        commentsPagingResult: CommentsPagingResult,
        selectedComments: List<SelectedComment>,
        batchModerationStatus: BatchModerationStatus,
        previousUiModelState: CommentsUiModel?,
        onToggle: (remoteCommentId: Long, commentStatus: CommentStatus) -> Unit,
        onClick: (comment: CommentEntity) -> Unit,
        onLoadNextPage: (offset: Int) -> Unit,
        onBatchModerationConfirmed: (comment: CommentStatus) -> Unit,
        onBatchModerationCancelled: () -> Unit
    ): CommentsUiModel {
        return CommentsUiModel(
            buildCommentList(
                commentsPagingResult,
                selectedComments,
                commentFilter,
                onToggle,
                onClick,
                onLoadNextPage
            ),
            buildCommentsListUiModel(
                commentsPagingResult,
                commentFilter,
                previousUiModelState
            ),
            buildActionModeUiModel(selectedComments, commentFilter),
            buildConfirmationDialogUiState(
                batchModerationStatus,
                selectedComments,
                onBatchModerationConfirmed,
                onBatchModerationCancelled
            )
        )
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

    sealed class BatchModerationStatus {
        data class AskingToModerate(val commentStatus: CommentStatus) : BatchModerationStatus()
        object Idle : BatchModerationStatus()
    }

    sealed class ConfirmationDialogUiModel {
        object Hidden : ConfirmationDialogUiModel()
        data class Visible(
            val title: Int,
            val message: Int,
            val positiveButton: Int,
            val negativeButton: Int,
            val confirmAction: () -> Unit,
            val cancelAction: () -> Unit
        ) : ConfirmationDialogUiModel()
    }

    data class CommentsUiModel(
        val commentData: CommentList,
        val commentsListUiModel: CommentsListUiModel,
        val actionModeUiModel: ActionModeUiModel,
        val confirmationDialogUiModel: ConfirmationDialogUiModel
    ) {
        companion object {
            fun buildInitialState(): CommentsUiModel {
                return CommentsUiModel(
                    commentData = CommentList(emptyList(), false),
                    commentsListUiModel = CommentsListUiModel.Loading,
                    actionModeUiModel = Hidden,
                    confirmationDialogUiModel = ConfirmationDialogUiModel.Hidden
                )
            }
        }
    }

    internal fun buildActionModeUiModel(
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

        val approveActionUiModel = ActionUiModel(
            true,
            selectedComments.any { it.status == UNAPPROVED || it.status == CommentStatus.SPAM })
        val unaproveActionUiModel = ActionUiModel(
            true,
            (commentListFilter != TRASHED && commentListFilter != SPAM && commentListFilter != PENDING) &&
                    selectedComments.any { it.status == CommentStatus.APPROVED }
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

    data class CommentList(val comments: List<UnifiedCommentListItem>, val hasMore: Boolean)

    internal fun buildConfirmationDialogUiState(
        batchModerationStatus: BatchModerationStatus,
        selectedComments: List<SelectedComment>,
        onModerateComments: (comment: CommentStatus) -> Unit,
        onCancel: () -> Unit
    ): ConfirmationDialogUiModel {
        if (batchModerationStatus is AskingToModerate) {
            return when (batchModerationStatus.commentStatus) {
                DELETED -> {
                    val messageResId = if (selectedComments.size > 1) {
                        string.dlg_sure_to_delete_comments
                    } else {
                        string.dlg_sure_to_delete_comment
                    }
                    ConfirmationDialogUiModel.Visible(
                        title = string.delete,
                        message = messageResId,
                        positiveButton = string.yes,
                        negativeButton = string.no,
                        confirmAction = {
                            onModerateComments.invoke(batchModerationStatus.commentStatus)
                        },
                        cancelAction = {
                            onCancel.invoke()
                        }
                    )
                }
                TRASH -> {
                    ConfirmationDialogUiModel.Visible(
                        title = string.trash,
                        message = string.dlg_confirm_trash_comments,
                        positiveButton = string.dlg_confirm_action_trash,
                        negativeButton = string.dlg_cancel_action_dont_trash,
                        confirmAction = {
                            onModerateComments.invoke(batchModerationStatus.commentStatus)
                        },
                        cancelAction = {
                            onCancel.invoke()
                        }
                    )
                }
                else -> {
                    ConfirmationDialogUiModel.Hidden
                }
            }
        } else {
            return ConfirmationDialogUiModel.Hidden
        }
    }

    @Suppress("LongParameterList")
    internal fun buildCommentList(
        commentsDataResult: CommentsPagingResult,
        selectedComments: List<SelectedComment>?,
        commentFilter: CommentFilter,
        onToggle: (remoteCommentId: Long, commentStatus: CommentStatus) -> Unit,
        onClick: (comment: CommentEntity) -> Unit,
        onLoadNextPage: (offset: Int) -> Unit
    ): CommentList {
        val (comments, hasMore) = when (commentsDataResult) {
            is Failure -> Pair(
                commentsDataResult.cachedData.comments,
                commentsDataResult.cachedData.hasMore
            )
            is Loading -> Pair(listOf(), false)
            is Success -> Pair(commentsDataResult.data.comments, commentsDataResult.data.hasMore)
        }

        val list = ArrayList<UnifiedCommentListItem>()
        comments.forEachIndexed { index, commentModel ->
            val previousItem = comments.getOrNull(index - 1)
            if (previousItem == null) {
                list.add(SubHeader(getFormattedDate(commentModel), -1))
            } else if (shouldAddSeparator(previousItem, commentModel)) {
                list.add(SubHeader(getFormattedDate(commentModel), -1))
            }

            val toggleAction = ToggleAction(commentModel, onToggle)
            val clickAction = ClickAction(commentModel, onClick)

            val isSelected = selectedComments?.any { it.remoteCommentId == commentModel.remoteCommentId }
            val isPending = commentModel.status == UNAPPROVED.toString()

            list.add(
                Comment(
                    remoteCommentId = commentModel.remoteCommentId,
                    postTitle = commentModel.postTitle.orEmpty(),
                    authorName = commentModel.authorName.orEmpty(),
                    authorEmail = commentModel.authorEmail.orEmpty(),
                    content = commentModel.content.orEmpty(),
                    publishedDate = commentModel.datePublished.orEmpty(),
                    publishedTimestamp = commentModel.publishedTimestamp,
                    authorAvatarUrl = commentModel.authorProfileImageUrl.orEmpty(),
                    isPending = isPending,
                    isSelected = isSelected ?: false,
                    clickAction = clickAction,
                    toggleAction = toggleAction
                )
            )
        }

        if (comments.isNotEmpty() && hasMore && commentFilter != UNREPLIED) {
            list.add(NextPageLoader(hasMore && commentsDataResult !is Failure, -1) {
                onLoadNextPage.invoke(comments.size)
            })
        }
        return CommentList(list.toImmutableList(), hasMore)
    }

    private fun shouldAddSeparator(before: CommentEntity, after: CommentEntity): Boolean {
        return getFormattedDate(before) != getFormattedDate(after)
    }

    private fun getFormattedDate(comment: CommentEntity): String {
        return dateTimeUtilsWrapper.javaDateToTimeSpan(DateTimeUtils.dateFromIso8601(comment.datePublished))
    }

    internal fun buildCommentsListUiModel(
        commentsDataResult: CommentsPagingResult,
        commentFilter: CommentFilter,
        previousState: CommentsUiModel?
    ): CommentsListUiModel {
        return when (commentsDataResult) {
            is Loading -> {
                if (previousState != null && previousState.commentData.comments.isNotEmpty()) {
                    CommentsListUiModel.Refreshing
                } else {
                    CommentsListUiModel.Loading
                }
            }
            is Success -> {
                if (commentsDataResult.data.comments.isEmpty()) {
                    CommentsListUiModel.Empty(
                        UiStringRes(getEmptyViewMessageResId(commentFilter)),
                        R.drawable.img_illustration_empty_results_216dp
                    )
                } else {
                    WithData
                }
            }
            is Failure -> {
                val errorMessage = commentsDataResult.error.message
                if (commentsDataResult.cachedData.comments.isEmpty()) {
                    val errorString = if (!networkUtilsWrapper.isNetworkAvailable()) {
                        UiStringRes(string.no_network_message)
                    } else if (errorMessage.isNullOrEmpty()) {
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

    fun getEmptyViewMessageResId(commentFilter: CommentFilter): Int {
        return when (commentFilter) {
            CommentFilter.APPROVED -> string.comments_empty_list_filtered_approved
            PENDING -> string.comments_empty_list_filtered_pending
            UNREPLIED -> string.comments_empty_list_filtered_unreplied
            SPAM -> string.comments_empty_list_filtered_spam
            TRASHED -> string.comments_empty_list_filtered_trashed
            CommentFilter.DELETE, CommentFilter.ALL -> string.comments_empty_list
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
}
