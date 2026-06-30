package org.wordpress.android.ui.comments.unified

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.DELETED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateHandler
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.Close
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.LaunchEditComment
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.OpenPostInReader
import org.wordpress.android.ui.comments.unified.CommentDetailsActionEvent.ReplySent
import org.wordpress.android.ui.comments.unified.CommentIdentifier.SiteCommentIdentifier
import org.wordpress.android.ui.comments.unified.usecase.GetCommentUseCase
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

/**
 * ViewModel for the unified comment detail screen (site-comments path).
 *
 * Loads a single comment via [GetCommentUseCase] and performs moderation, like and edit actions.
 * After any change it requests a local cache update so the comment list refreshes automatically
 * (the list subscribes to [LocalCommentCacheUpdateHandler]).
 *
 * Notification-sourced comments, reply, suggestions and the more-menu (copy/share link) are handled
 * in later phases of the Comments Unification migration.
 */
class UnifiedCommentDetailsViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val commentsStore: CommentsStore,
    private val getCommentUseCase: GetCommentUseCase,
    private val localCommentCacheUpdateHandler: LocalCommentCacheUpdateHandler,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<CommentDetailsUiState>()
    private val _uiActionEvent = MutableLiveData<Event<CommentDetailsActionEvent>>()
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()

    val uiState: LiveData<CommentDetailsUiState> = _uiState
    val uiActionEvent: LiveData<Event<CommentDetailsActionEvent>> = _uiActionEvent
    val onSnackbarMessage: LiveData<Event<SnackbarMessageHolder>> = _onSnackbarMessage

    private var isStarted = false
    private lateinit var site: SiteModel
    private var remoteCommentId: Long = 0
    private var loadedComment: CommentEntity? = null

    fun start(site: SiteModel, remoteCommentId: Long) {
        if (isStarted) return
        isStarted = true
        this.site = site
        this.remoteCommentId = remoteCommentId
        loadComment()
    }

    /**
     * Reloads the comment from the store. Used after returning from the edit screen so any edits
     * are reflected here.
     */
    fun refreshComment() {
        if (!isStarted) return
        loadComment()
    }

    private fun loadComment() {
        launch {
            _uiState.value = CommentDetailsUiState(showProgress = true)
            val comment = withContext(bgDispatcher) {
                getCommentUseCase.execute(site, remoteCommentId)
            }
            if (comment != null) {
                loadedComment = comment
                _uiState.value = comment.toUiState()
            } else {
                _onSnackbarMessage.value = Event(
                    SnackbarMessageHolder(
                        message = UiStringRes(R.string.error_load_comment),
                        onDismissAction = { _uiActionEvent.value = Event(Close) }
                    )
                )
            }
        }
    }

    fun onApproveClicked() {
        val newStatus = if (currentStatus() == APPROVED) UNAPPROVED else APPROVED
        moderateComment(newStatus, closeOnSuccess = false)
    }

    fun onSpamClicked() {
        val newStatus = if (currentStatus() == SPAM) APPROVED else SPAM
        moderateComment(newStatus, closeOnSuccess = newStatus == SPAM)
    }

    fun onTrashClicked() {
        val newStatus = if (currentStatus() == TRASH) APPROVED else TRASH
        moderateComment(newStatus, closeOnSuccess = newStatus == TRASH)
    }

    fun onDeletePermanentlyClicked() {
        moderateComment(DELETED, closeOnSuccess = true)
    }

    fun onLikeClicked() {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            showSnackbar(R.string.no_network_message)
            return
        }
        val isLike = !(_uiState.value?.isLiked ?: false)
        launch {
            _uiState.value = _uiState.value?.copy(isLiked = isLike)
            val isError = withContext(bgDispatcher) {
                commentsStore.likeComment(site, remoteCommentId, null, isLike).isError
            }
            if (isError) {
                _uiState.value = _uiState.value?.copy(isLiked = !isLike)
                showSnackbar(R.string.error_generic)
            } else {
                withContext(bgDispatcher) { localCommentCacheUpdateHandler.requestCommentsUpdate() }
            }
        }
    }

    fun onEditClicked() {
        val comment = loadedComment ?: return
        _uiActionEvent.value = Event(
            LaunchEditComment(site, SiteCommentIdentifier(comment.id.toInt(), remoteCommentId))
        )
    }

    fun onPostTitleClicked() {
        val comment = loadedComment ?: return
        if (comment.remotePostId <= 0) return
        _uiActionEvent.value = Event(OpenPostInReader(site.siteId, comment.remotePostId))
    }

    fun onReplyClicked(replyText: String) {
        if (replyText.isBlank()) return
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            showSnackbar(R.string.no_network_message)
            return
        }
        val parent = loadedComment ?: return
        launch {
            _uiState.value = _uiState.value?.copy(isReplyInProgress = true)
            val isError = withContext(bgDispatcher) {
                val result = commentsStore.createNewReply(site, parent, buildReply(parent, replyText))
                if (!result.isError) {
                    localCommentCacheUpdateHandler.requestCommentsUpdate()
                }
                result.isError
            }
            _uiState.value = _uiState.value?.copy(isReplyInProgress = false)
            if (isError) {
                showSnackbar(R.string.error_generic)
            } else {
                // Replying to an unapproved comment implicitly approves it, matching legacy behaviour
                if (currentStatus() == UNAPPROVED) {
                    approveAfterReply()
                }
                _uiActionEvent.value = Event(ReplySent)
                showSnackbar(R.string.note_reply_successful)
            }
        }
    }

    private suspend fun approveAfterReply() {
        _uiState.value = _uiState.value?.copy(status = APPROVED)
        val isError = withContext(bgDispatcher) {
            moderateOnStore(APPROVED, UNAPPROVED)
        }
        if (isError) {
            _uiState.value = _uiState.value?.copy(status = UNAPPROVED)
        }
    }

    private fun buildReply(parent: CommentEntity, replyText: String) = CommentEntity(
        remoteCommentId = 0,
        remotePostId = parent.remotePostId,
        localSiteId = site.id,
        remoteSiteId = site.siteId,
        authorUrl = null,
        authorName = null,
        authorEmail = null,
        authorProfileImageUrl = null,
        authorId = 0,
        postTitle = null,
        status = null,
        datePublished = null,
        publishedTimestamp = 0,
        content = replyText,
        url = null,
        hasParent = true,
        parentId = parent.remoteCommentId,
        iLike = false
    )

    private fun moderateComment(newStatus: CommentStatus, closeOnSuccess: Boolean) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            showSnackbar(R.string.no_network_message)
            return
        }
        val previousStatus = currentStatus()
        launch {
            _uiState.value = _uiState.value?.copy(status = newStatus)
            val isError = withContext(bgDispatcher) {
                moderateOnStore(newStatus, previousStatus)
            }
            if (isError) {
                _uiState.value = _uiState.value?.copy(status = previousStatus)
                showSnackbar(R.string.error_moderate_comment)
            } else if (closeOnSuccess) {
                _uiActionEvent.value = Event(Close)
            }
        }
    }

    private suspend fun moderateOnStore(newStatus: CommentStatus, fallbackStatus: CommentStatus): Boolean {
        commentsStore.moderateCommentLocally(site, remoteCommentId, newStatus)
        localCommentCacheUpdateHandler.requestCommentsUpdate()
        val result = if (newStatus == DELETED) {
            commentsStore.deleteComment(site, remoteCommentId, null)
        } else {
            commentsStore.pushLocalCommentByRemoteId(site, remoteCommentId)
        }
        if (result.isError) {
            commentsStore.moderateCommentLocally(site, remoteCommentId, fallbackStatus)
        }
        localCommentCacheUpdateHandler.requestCommentsUpdate()
        return result.isError
    }

    private fun currentStatus(): CommentStatus = _uiState.value?.status ?: CommentStatus.ALL

    private fun formatDate(isoDate: String?): String {
        if (isoDate.isNullOrEmpty()) return ""
        return dateTimeUtilsWrapper.javaDateToTimeSpan(dateTimeUtilsWrapper.dateFromIso8601(isoDate))
    }

    private fun showSnackbar(messageRes: Int) {
        _onSnackbarMessage.value = Event(SnackbarMessageHolder(UiStringRes(messageRes)))
    }

    private fun CommentEntity.toUiState() = CommentDetailsUiState(
        showProgress = false,
        contentVisible = true,
        authorName = authorName ?: "",
        authorAvatarUrl = authorProfileImageUrl ?: "",
        datePublished = formatDate(datePublished),
        commentText = content ?: "",
        postTitle = postTitle ?: "",
        commentUrl = url ?: "",
        status = CommentStatus.fromString(status),
        isLiked = iLike
    )

    data class CommentDetailsUiState(
        val showProgress: Boolean = false,
        val contentVisible: Boolean = false,
        val authorName: String = "",
        val authorAvatarUrl: String = "",
        val datePublished: String = "",
        val commentText: String = "",
        val postTitle: String = "",
        val commentUrl: String = "",
        val status: CommentStatus = CommentStatus.ALL,
        val isLiked: Boolean = false,
        val isReplyInProgress: Boolean = false
    )
}

sealed class CommentDetailsActionEvent {
    object Close : CommentDetailsActionEvent()
    object ReplySent : CommentDetailsActionEvent()
    data class LaunchEditComment(
        val site: SiteModel,
        val commentIdentifier: CommentIdentifier
    ) : CommentDetailsActionEvent()
    data class OpenPostInReader(
        val blogId: Long,
        val postId: Long
    ) : CommentDetailsActionEvent()
}
