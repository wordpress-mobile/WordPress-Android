package org.wordpress.android.ui.comments.unified

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.datasets.wrappers.ReaderCommentTableWrapper
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.models.ReaderComment
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateHandler
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.comments.unified.CommentIdentifier.ReaderCommentIdentifier
import org.wordpress.android.ui.comments.unified.CommentIdentifier.SiteCommentIdentifier
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentActionEvent.CANCEL_EDIT_CONFIRM
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentActionEvent.CLOSE
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.EditCommentActionEvent.DONE
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.FieldType.COMMENT
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.FieldType.USER_EMAIL
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.FieldType.USER_NAME
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.FieldType.WEB_ADDRESS
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.ProgressState.LOADING
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.ProgressState.NOT_VISIBLE
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel.ProgressState.SAVING
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.actions.ReaderCommentActionsWrapper
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.validateEmail
import org.wordpress.android.util.validateUrl
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UnifiedCommentsEditViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val commentsStore: CommentsStore,
    private val resourceProvider: ResourceProvider,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerCommentTableWrapper: ReaderCommentTableWrapper,
    private val readerCommentActionsWrapper: ReaderCommentActionsWrapper,
    private val localCommentCacheUpdateHandler: LocalCommentCacheUpdateHandler
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<EditCommentUiState>()
    private val _uiActionEvent = MutableLiveData<Event<EditCommentActionEvent>>()
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()

    val uiState: LiveData<EditCommentUiState> = _uiState
    val uiActionEvent: LiveData<Event<EditCommentActionEvent>> = _uiActionEvent
    val onSnackbarMessage: LiveData<Event<SnackbarMessageHolder>> = _onSnackbarMessage

    private var isStarted = false
    private lateinit var site: SiteModel
    private lateinit var commentIdentifier: CommentIdentifier

    data class EditErrorStrings(
        val userNameError: String? = null,
        val commentTextError: String? = null,
        val userUrlError: String? = null,
        val userEmailError: String? = null
    )

    data class CommentEssentials(
        val commentId: Long = 0,
        val userName: String = "",
        val commentText: String = "",
        val userUrl: String = "",
        val userEmail: String = ""
    )

    data class EditCommentUiState(
        val canSaveChanges: Boolean,
        val shouldInitComment: Boolean,
        val shouldInitWatchers: Boolean,
        val showProgress: Boolean = false,
        val progressText: UiString? = null,
        val originalComment: CommentEssentials,
        val editedComment: CommentEssentials,
        val editErrorStrings: EditErrorStrings
    )

    enum class ProgressState(val show: Boolean, val progressText: UiString?) {
        NOT_VISIBLE(false, null),
        LOADING(true, UiStringRes(R.string.loading)),
        SAVING(true, UiStringRes(R.string.saving_changes))
    }

    enum class FieldType(@StringRes val errorStringRes: Int, val isValid: (String) -> Boolean) {
        USER_NAME(R.string.comment_edit_user_name_error, { isValidUserName(it) }),
        USER_EMAIL(R.string.comment_edit_user_email_error, { isValidUserEmail(it) }),
        WEB_ADDRESS(R.string.comment_edit_web_address_error, { isValidWebAddress(it) }),
        COMMENT(R.string.comment_edit_comment_error, { isValidComment(it) });

        // This is here for testing purposes
        fun matches(expectedField: FieldType): Boolean {
            return this == expectedField
        }

        companion object {
            private fun isValidUserName(userName: String): Boolean {
                return userName.isNotBlank()
            }

            private fun isValidUserEmail(email: String): Boolean {
                return email.isBlank() || validateEmail(email)
            }

            private fun isValidWebAddress(url: String): Boolean {
                return url.isBlank() || validateUrl(url)
            }

            private fun isValidComment(comment: String): Boolean {
                return comment.isNotBlank()
            }
        }
    }

    enum class EditCommentActionEvent {
        CLOSE,
        DONE,
        CANCEL_EDIT_CONFIRM
    }

    fun start(site: SiteModel, commentId: Int) {
        if (isStarted) {
            // If we are here, the fragment view was recreated (like in a configuration change)
            // so we reattach the watchers.
            _uiState.value = _uiState.value?.copy(shouldInitWatchers = true)
            return
        }
        isStarted = true

        this.site = site

        initViews(commentId)
    }

    private suspend fun setLoadingState(state: ProgressState) {
        val uiState = _uiState.value ?: EditCommentUiState(
                canSaveChanges = false,
                shouldInitComment = false,
                shouldInitWatchers = false,
                showProgress = LOADING.show,
                progressText = LOADING.progressText,
                originalComment = CommentEssentials(),
                editedComment = CommentEssentials(),
                editErrorStrings = EditErrorStrings()
        )

        withContext(mainDispatcher) {
            _uiState.value = uiState.copy(
                    showProgress = state.show,
                    progressText = state.progressText
            )
        }
    }

    fun onActionMenuClicked() {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _onSnackbarMessage.value = Event(SnackbarMessageHolder(UiStringRes(R.string.no_network_message)))
            return
        }

        _uiState.value?.let { uiState ->
            val editedContent = uiState.editedComment

            launch(bgDispatcher) {
                setLoadingState(SAVING)

                if (commentIdentifier is SiteCommentIdentifier) {
                    val comment = commentsStore.getCommentByLocalId(editedContent.commentId).firstOrNull()

                    comment?.let {
                        val updatedComment = comment.copy(
                                authorUrl = editedContent.userUrl,
                                authorName = editedContent.userName,
                                authorEmail = editedContent.userEmail,
                                content = editedContent.commentText
                        )
                        val result = commentsStore.updateEditComment(site, updatedComment)

                        if (result.isError) {
                            setLoadingState(NOT_VISIBLE)
                            _onSnackbarMessage.postValue(
                                    Event(SnackbarMessageHolder(UiStringRes(R.string.error_edit_comment)))
                            )
                        } else {
                            _uiActionEvent.postValue(Event(DONE))
                            localCommentCacheUpdateHandler.requestCommentsUpdate()
                        }
                    }
                } else if (commentIdentifier is ReaderCommentIdentifier) {
                    val readerCommentIdentifier = commentIdentifier as ReaderCommentIdentifier

                    val comment = readerCommentTableWrapper.getComment(
                            readerCommentIdentifier.blogId,
                            readerCommentIdentifier.postId,
                            readerCommentIdentifier.remoteCommentId
                    )

                    comment?.let {
                        comment.authorUrl = editedContent.userUrl
                        comment.authorName = editedContent.userName
                        comment.authorEmail = editedContent.userEmail
                        comment.text = editedContent.commentText

                        val commentUpdateResult = updatedComment(comment)

                        if (commentUpdateResult) {
                            _uiActionEvent.postValue(Event(DONE))
                        } else {
                            setLoadingState(NOT_VISIBLE)
                            _onSnackbarMessage.postValue(
                                    Event(SnackbarMessageHolder(UiStringRes(R.string.error_edit_comment)))
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun updatedComment(comment: ReaderComment): Boolean {
        return suspendCoroutine { continuation ->
            readerCommentActionsWrapper.updateComment(
                    comment,
                    listener = { succeeded, newComment ->
                        continuation.resume(succeeded)
                    })
        }
    }

    fun onBackPressed() {
        _uiState.value?.let {
            if (it.editedComment.isNotEqualTo(it.originalComment)) {
                _uiActionEvent.value = Event(CANCEL_EDIT_CONFIRM)
            } else {
                _uiActionEvent.value = Event(CLOSE)
            }
        }
    }

    fun onConfirmEditingDiscard() {
        _uiActionEvent.value = Event(CLOSE)
    }

    private fun initViews() {
        launch {
            setLoadingState(LOADING)

            if (commentIdentifier is SiteCommentIdentifier) {
                val siteCommentIdentifier = commentIdentifier as SiteCommentIdentifier
                val commentList = withContext(bgDispatcher) {
                    commentsStore.getCommentByLocalId(siteCommentIdentifier.localCommentId.toLong())
                }

                if (commentList.isEmpty()) {
                    _onSnackbarMessage.value = Event(SnackbarMessageHolder(
                            message = UiStringRes(R.string.error_load_comment),
                            onDismissAction = { _ ->
                                _uiActionEvent.value = Event(CLOSE)
                            }
                    ))
                    return@launch
                } else {
                    val comment = commentList.first()
                    val commentEssentials = CommentEssentials(
                            commentId = comment.id,
                            userName = comment.authorName ?: "",
                            commentText = comment.content ?: "",
                            userUrl = comment.authorUrl ?: "",
                            userEmail = comment.authorEmail ?: ""
                    )
                    _uiState.value = EditCommentUiState(
                            canSaveChanges = false,
                            shouldInitComment = true,
                            shouldInitWatchers = true,
                            showProgress = LOADING.show,
                            progressText = LOADING.progressText,
                            originalComment = commentEssentials,
                            editedComment = commentEssentials,
                            editErrorStrings = EditErrorStrings()
                    )
                }
            } else if (commentIdentifier is ReaderCommentIdentifier) {
                val readerCommentIdentifier = commentIdentifier as ReaderCommentIdentifier
                val comment = readerCommentTableWrapper.getComment(
                        readerCommentIdentifier.blogId,
                        readerCommentIdentifier.postId,
                        readerCommentIdentifier.remoteCommentId
                )
                if (comment == null) {
                    _onSnackbarMessage.value = Event(SnackbarMessageHolder(
                            message = UiStringRes(R.string.error_load_comment),
                            onDismissAction = { _ ->
                                _uiActionEvent.value = Event(CLOSE)
                            }
                    ))
                    return@launch
                } else {
                    val commentEssentials = CommentEssentials(
                            commentId = comment.commentId,
                            userName = comment.authorName ?: "",
                            commentText = comment.text ?: "",
                            userUrl = comment.authorUrl ?: "",
                            userEmail = comment.authorEmail ?: ""
                    )
                    _uiState.value = EditCommentUiState(
                            canSaveChanges = false,
                            shouldInitComment = true,
                            shouldInitWatchers = true,
                            showProgress = LOADING.show,
                            progressText = LOADING.progressText,
                            originalComment = commentEssentials,
                            editedComment = commentEssentials,
                            editErrorStrings = EditErrorStrings()
                    )
                }
            }

            delay(LOADING_DELAY_MS)
            setLoadingState(NOT_VISIBLE)
        }
    }

    fun onValidateField(field: String, fieldType: FieldType) {
        _uiState.value?.let {
            val fieldError = if (fieldType.isValid.invoke(field)) {
                null
            } else {
                resourceProvider.getString(fieldType.errorStringRes)
            }

            val previousComment = it.editedComment
            val previousErrors = it.editErrorStrings

            val editedComment = previousComment.copy(
                    userName = if (fieldType.matches(USER_NAME)) field else previousComment.userName,
                    commentText = if (fieldType.matches(COMMENT)) field else previousComment.commentText,
                    userUrl = if (fieldType.matches(WEB_ADDRESS)) field else previousComment.userUrl,
                    userEmail = if (fieldType.matches(USER_EMAIL)) field else previousComment.userEmail
            )

            val errors = previousErrors.copy(
                    userNameError = if (fieldType.matches(USER_NAME)) fieldError else previousErrors.userNameError,
                    commentTextError = if (fieldType.matches(COMMENT)) fieldError else previousErrors.commentTextError,
                    userUrlError = if (fieldType.matches(WEB_ADDRESS)) fieldError else previousErrors.userUrlError,
                    userEmailError = if (fieldType.matches(USER_EMAIL)) fieldError else previousErrors.userEmailError
            )

            _uiState.value = it.copy(
                    canSaveChanges = editedComment.isNotEqualTo(it.originalComment) && !errors.hasError(),
                    shouldInitComment = false,
                    shouldInitWatchers = false,
                    editedComment = editedComment,
                    editErrorStrings = errors
            )
        }
    }

    private fun CommentEssentials.isNotEqualTo(other: CommentEssentials): Boolean {
        return !(this.commentText == other.commentText &&
                this.userEmail == other.userEmail &&
                this.userName == other.userName &&
                this.userUrl == other.userUrl)
    }

    private fun EditErrorStrings.hasError(): Boolean {
        return listOf(
                this.commentTextError,
                this.userEmailError,
                this.userNameError,
                this.userUrlError
        ).any { !it.isNullOrEmpty() }
    }

    companion object {
        private const val LOADING_DELAY_MS = 300L
    }
}
