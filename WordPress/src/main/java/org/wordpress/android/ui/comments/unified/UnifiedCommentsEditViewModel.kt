package org.wordpress.android.ui.comments.unified

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.COMMENT_EDITED
import org.wordpress.android.datasets.wrappers.ReaderCommentTableWrapper
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateHandler
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.comments.unified.CommentIdentifier.NotificationCommentIdentifier
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
import org.wordpress.android.ui.comments.unified.extension.isNotEqualTo
import org.wordpress.android.ui.comments.unified.usecase.GetCommentUseCase
import org.wordpress.android.ui.notifications.utils.NotificationsActionsWrapper
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsCommentActionSource
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.util.validateEmail
import org.wordpress.android.util.validateUrl
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class UnifiedCommentsEditViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val commentsStore: CommentsStore,
    private val resourceProvider: ResourceProvider,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val localCommentCacheUpdateHandler: LocalCommentCacheUpdateHandler,
    private val getCommentUseCase: GetCommentUseCase,
    private val notificationActionsWrapper: NotificationsActionsWrapper,
    private val readerCommentTableWrapper: ReaderCommentTableWrapper,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper
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

    data class EditCommentUiState(
        val canSaveChanges: Boolean,
        val shouldInitComment: Boolean,
        val shouldInitWatchers: Boolean,
        val showProgress: Boolean = false,
        val progressText: UiString? = null,
        val originalComment: CommentEssentials,
        val editedComment: CommentEssentials,
        val editErrorStrings: EditErrorStrings,
        val inputSettings: InputSettings
    )

    data class InputSettings(
        val enableEditName: Boolean,
        val enableEditUrl: Boolean,
        val enableEditEmail: Boolean,
        val enableEditComment: Boolean
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

    fun start(site: SiteModel, commentIdentifier: CommentIdentifier) {
        if (isStarted) {
            // If we are here, the fragment view was recreated (like in a configuration change)
            // so we reattach the watchers.
            _uiState.value = _uiState.value?.copy(shouldInitWatchers = true)
            return
        }
        isStarted = true

        this.site = site
        this.commentIdentifier = commentIdentifier

        initViews()
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
                editErrorStrings = EditErrorStrings(),
                inputSettings = mapInputSettings(CommentEssentials())
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
            val editedCommentEssentials = uiState.editedComment
            launch(bgDispatcher) {
                setLoadingState(SAVING)
                updateComment(editedCommentEssentials)
            }
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

            val commentEssentials = withContext(bgDispatcher) {
                mapCommentEssentials()
            }
            if (commentEssentials.isValid()) {
                _uiState.value =
                        EditCommentUiState(
                                canSaveChanges = false,
                                shouldInitComment = true,
                                shouldInitWatchers = true,
                                showProgress = LOADING.show,
                                progressText = LOADING.progressText,
                                originalComment = commentEssentials,
                                editedComment = commentEssentials,
                                editErrorStrings = EditErrorStrings(),
                                inputSettings = mapInputSettings(commentEssentials)
                        )
            } else {
                _onSnackbarMessage.value = Event(SnackbarMessageHolder(
                        message = UiStringRes(R.string.error_load_comment),
                        onDismissAction = { _uiActionEvent.value = Event(CLOSE) }
                ))
            }
            delay(LOADING_DELAY_MS)
            setLoadingState(NOT_VISIBLE)
        }
    }

    private suspend fun mapCommentEssentials(): CommentEssentials {
        val commentEntity = getCommentUseCase.execute(site, commentIdentifier.remoteCommentId)
        return if (commentEntity != null) {
            CommentEssentials(
                    commentId = commentEntity.id,
                    userName = commentEntity.authorName ?: "",
                    commentText = commentEntity.content ?: "",
                    userUrl = commentEntity.authorUrl ?: "",
                    userEmail = commentEntity.authorEmail ?: "",
                    isFromRegisteredUser = commentEntity.authorId > 0
            )
        } else {
            CommentEssentials()
        }
    }

    private suspend fun updateComment(editedCommentEssentials: CommentEssentials) {
        val commentEntity =
                commentsStore.getCommentByLocalSiteAndRemoteId(site.id, commentIdentifier.remoteCommentId).firstOrNull()
        commentEntity?.run {
            val isCommentEntityUpdated = updateCommentEntity(this, editedCommentEssentials)
            if (isCommentEntityUpdated) {
                analyticsUtilsWrapper.trackCommentActionWithSiteDetails(
                        COMMENT_EDITED,
                        commentIdentifier.toCommentActionSource(),
                        site
                )
                when (commentIdentifier) {
                    is NotificationCommentIdentifier -> {
                        updateNotificationEntity()
                    }
                    is ReaderCommentIdentifier -> {
                        updateReaderEntity(editedCommentEssentials)
                    }
                    else -> {
                        _uiActionEvent.postValue(Event(DONE))
                        localCommentCacheUpdateHandler.requestCommentsUpdate()
                    }
                }
            } else {
                showUpdateCommentError()
            }
        } ?: showUpdateCommentError()
    }

    private suspend fun updateCommentEntity(
        comment: CommentEntity,
        editedCommentEssentials: CommentEssentials
    ): Boolean {
        val updatedComment = comment.copy(
                authorUrl = editedCommentEssentials.userUrl,
                authorName = editedCommentEssentials.userName,
                authorEmail = editedCommentEssentials.userEmail,
                content = editedCommentEssentials.commentText
        )

        val result = commentsStore.updateEditComment(site, updatedComment)
        return !result.isError
    }

    private suspend fun updateNotificationEntity() {
        with(commentIdentifier as NotificationCommentIdentifier) {
            val isNotificationEntityUpdated = notificationActionsWrapper.downloadNoteAndUpdateDB(noteId)
            if (isNotificationEntityUpdated) {
                _uiActionEvent.postValue(Event(DONE))
                localCommentCacheUpdateHandler.requestCommentsUpdate()
            } else {
                showUpdateNotificationError()
            }
        }
    }

    private suspend fun updateReaderEntity(commentEssentials: CommentEssentials) {
        val readerCommentIdentifier = commentIdentifier as ReaderCommentIdentifier

        val readerComment = readerCommentTableWrapper.getComment(
                site.siteId,
                readerCommentIdentifier.postId,
                readerCommentIdentifier.remoteCommentId
        )

        readerComment?.apply {
            text = commentEssentials.commentText
            authorName = commentEssentials.userName
            authorEmail = commentEssentials.userEmail
            authorUrl = commentEssentials.userUrl
            readerCommentTableWrapper.addOrUpdateComment(readerComment)
        }
        _uiActionEvent.postValue(Event(DONE))
        localCommentCacheUpdateHandler.requestCommentsUpdate()
    }

    private suspend fun showUpdateCommentError() {
        setLoadingState(NOT_VISIBLE)
        _onSnackbarMessage.postValue(
                Event(SnackbarMessageHolder(UiStringRes(R.string.error_edit_comment)))
        )
    }

    private suspend fun showUpdateNotificationError() {
        setLoadingState(NOT_VISIBLE)
        _onSnackbarMessage.postValue(
                Event(SnackbarMessageHolder(UiStringRes(R.string.error_edit_notification)))
        )
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

    private fun mapInputSettings(commentEssentials: CommentEssentials) = InputSettings(
            enableEditName = !commentEssentials.isFromRegisteredUser,
            enableEditUrl = !commentEssentials.isFromRegisteredUser,
            enableEditEmail = !commentEssentials.isFromRegisteredUser,
            enableEditComment = true
    )

    private fun EditErrorStrings.hasError(): Boolean {
        return listOf(
                this.commentTextError,
                this.userEmailError,
                this.userNameError,
                this.userUrlError
        ).any { !it.isNullOrEmpty() }
    }

    private fun CommentIdentifier.toCommentActionSource(): AnalyticsCommentActionSource {
        return when (this) {
            is NotificationCommentIdentifier -> {
                AnalyticsCommentActionSource.NOTIFICATIONS
            }
            is ReaderCommentIdentifier -> {
                AnalyticsCommentActionSource.READER
            }
            is SiteCommentIdentifier -> {
                AnalyticsCommentActionSource.SITE_COMMENTS
            }
        }
    }

    companion object {
        private const val LOADING_DELAY_MS = 300L
    }
}
