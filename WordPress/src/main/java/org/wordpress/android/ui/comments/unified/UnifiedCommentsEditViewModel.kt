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
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateHandler
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsEditResult
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.comments.unified.CommentIdentifier.NotificationCommentIdentifier
import org.wordpress.android.ui.comments.unified.CommentIdentifier.ReaderCommentIdentifier
import org.wordpress.android.ui.comments.unified.CommentIdentifier.SiteCommentIdentifier
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
    private val commentsRsDataSource: CommentsRsDataSource,
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

    // Written on the main thread in onActionMenuClicked and cleared by the save coroutine;
    // @Volatile covers the cross-thread clear.
    @Volatile
    private var isSaving = false

    private lateinit var site: SiteModel

    private lateinit var commentIdentifier: CommentIdentifier

    data class EditErrorStrings(
        val userNameError: String? = null,
        val commentTextError: String? = null,
        val userUrlError: String? = null,
        val userEmailError: String? = null
    )

    data class EditCommentUiState(
        val canSaveChanges: Boolean = false,
        val showProgress: Boolean = false,
        val progressText: UiString? = null,
        // Lives in ui state (not a one-shot event) so the dialog survives configuration changes,
        // like the DialogFragment it replaced.
        val showDiscardDialog: Boolean = false,
        val originalComment: CommentEssentials = CommentEssentials(),
        val editedComment: CommentEssentials = CommentEssentials(),
        val editErrorStrings: EditErrorStrings = EditErrorStrings()
    )

    enum class ProgressState(val show: Boolean, val progressText: UiString?) {
        NOT_VISIBLE(false, null),
        LOADING(true, UiStringRes(R.string.loading)),
        SAVING(true, UiStringRes(R.string.saving_changes))
    }

    enum class FieldType(@StringRes val errorStringRes: Int, val isValid: (String) -> Boolean) {
        USER_NAME(R.string.comment_edit_user_name_error, { Utilities.isValidUserName(it) }),
        USER_EMAIL(R.string.comment_edit_user_email_error, { Utilities.isValidUserEmail(it) }),
        WEB_ADDRESS(R.string.comment_edit_web_address_error, { Utilities.isValidWebAddress(it) }),
        COMMENT(R.string.comment_edit_comment_error, { Utilities.isValidComment(it) });

        // This is here for testing purposes
        fun matches(expectedField: FieldType): Boolean {
            return this == expectedField
        }

        private object Utilities {
            fun isValidUserName(userName: String): Boolean {
                return userName.isNotBlank()
            }

            fun isValidUserEmail(email: String): Boolean {
                return email.isBlank() || validateEmail(email)
            }

            fun isValidWebAddress(url: String): Boolean {
                return url.isBlank() || validateUrl(url)
            }

            fun isValidComment(comment: String): Boolean {
                return comment.isNotBlank()
            }
        }
    }

    enum class EditCommentActionEvent {
        CLOSE,
        DONE
    }

    fun start(site: SiteModel, commentIdentifier: CommentIdentifier) {
        if (isStarted) return
        isStarted = true

        this.site = site
        this.commentIdentifier = commentIdentifier

        initViews()
    }

    private suspend fun setLoadingState(state: ProgressState) {
        // showProgress/progressText are overwritten by the copy below, so the fallback only needs
        // the default field values.
        val uiState = _uiState.value ?: EditCommentUiState()

        withContext(mainDispatcher) {
            _uiState.value = uiState.copy(
                showProgress = state.show,
                progressText = state.progressText
            )
        }
    }

    fun onActionMenuClicked() {
        // Set on the main thread before the save launches, so this check-then-set is race-free:
        // it closes the window where a second tap lands before the SAVING state disables the
        // Save button.
        if (isSaving) return
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _onSnackbarMessage.value = Event(SnackbarMessageHolder(UiStringRes(R.string.no_network_message)))
            return
        }
        _uiState.value?.let { uiState ->
            val editedCommentEssentials = uiState.editedComment
            isSaving = true
            launch(bgDispatcher) {
                try {
                    setLoadingState(SAVING)
                    updateComment(editedCommentEssentials)
                } finally {
                    isSaving = false
                }
            }
        }
    }

    fun onBackPressed() {
        _uiState.value?.let {
            if (it.editedComment.isNotEqualTo(it.originalComment)) {
                _uiState.value = it.copy(showDiscardDialog = true)
            } else {
                _uiActionEvent.value = Event(CLOSE)
            }
        }
    }

    fun onDiscardDialogDismissed() {
        _uiState.value?.let { _uiState.value = it.copy(showDiscardDialog = false) }
    }

    fun onConfirmEditingDiscard() {
        _uiState.value?.let { _uiState.value = it.copy(showDiscardDialog = false) }
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
                        showProgress = LOADING.show,
                        progressText = LOADING.progressText,
                        originalComment = commentEssentials,
                        editedComment = commentEssentials
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
        // A failed load returns default CommentEssentials, which fails isValid() and surfaces
        // the load-error snackbar in initViews().
        val essentials = if (canUseRs()) loadCommentViaRs() else loadCommentViaFluxC()
        return essentials ?: CommentEssentials()
    }

    // Edit context returns the raw (unrendered) content the editor must show — the same
    // thing the FluxC entity stored — plus the author email the view context omits.
    private suspend fun loadCommentViaRs(): CommentEssentials? =
        commentsRsDataSource.getCommentForEdit(site, commentIdentifier.remoteCommentId)?.let { rsComment ->
            CommentEssentials(
                commentId = commentIdentifier.remoteCommentId,
                userName = rsComment.authorName,
                commentText = rsComment.contentRaw,
                userUrl = rsComment.authorUrl,
                userEmail = rsComment.authorEmail
            )
        }

    private suspend fun loadCommentViaFluxC(): CommentEssentials? =
        getCommentUseCase.execute(site, commentIdentifier.remoteCommentId)?.let { commentEntity ->
            CommentEssentials(
                commentId = commentEntity.id,
                userName = commentEntity.authorName ?: "",
                commentText = commentEntity.content ?: "",
                userUrl = commentEntity.authorUrl ?: "",
                userEmail = commentEntity.authorEmail ?: ""
            )
        }

    private suspend fun updateComment(editedCommentEssentials: CommentEssentials) {
        // Prefer wordpress-rs, which can edit comments on both WP.com and self-hosted
        // application-password sites (FluxC's updateEditComment can't reach app-password sites).
        // Fall back to FluxC for sites rs can't serve — e.g. XML-RPC-only self-hosted comments
        // still reachable through the legacy detail/reader launch points.
        val saved = if (canUseRs()) {
            updateCommentViaRs(editedCommentEssentials)
        } else {
            updateCommentViaFluxC(editedCommentEssentials)
        }
        if (saved) {
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
    }

    private suspend fun updateCommentViaFluxC(editedCommentEssentials: CommentEssentials): Boolean {
        val comment = commentsStore
            .getCommentByLocalSiteAndRemoteId(site.id, commentIdentifier.remoteCommentId)
            .firstOrNull() ?: return false
        val updatedComment = comment.copy(
            authorUrl = editedCommentEssentials.userUrl,
            authorName = editedCommentEssentials.userName,
            authorEmail = editedCommentEssentials.userEmail,
            content = editedCommentEssentials.commentText
        )
        return !commentsStore.updateEditComment(site, updatedComment).isError
    }

    private fun canUseRs(): Boolean = site.isUsingWpComRestApi || site.hasApplicationPassword()

    /**
     * Saves the edit through wordpress-rs and, on success, mirrors the SERVER's resulting state
     * into the FluxC cache so the still-FluxC comment list/notifications reflect the change —
     * the same save-then-mirror pattern the unified comment detail uses for moderation. The
     * server echo (not the values that were sent) is what's cached so server-side normalisation
     * (e.g. KSES content filtering) can't diverge from the cache; the legacy FluxC path also
     * cached the server response.
     *
     * The mirror is best-effort: comments opened from the rs list on an application-password
     * site may have no FluxC row (FluxC can't fetch there), and the rs screens read the server,
     * not the cache.
     */
    private suspend fun updateCommentViaRs(editedCommentEssentials: CommentEssentials): Boolean {
        // The endpoint applies author fields to the comment record for any comment (registered
        // author or not), same as the legacy FluxC POST and wp-admin's comment editor.
        val result = commentsRsDataSource.updateComment(
            site = site,
            commentId = commentIdentifier.remoteCommentId,
            content = editedCommentEssentials.commentText,
            author = CommentsRsDataSource.CommentAuthor(
                name = editedCommentEssentials.userName,
                email = editedCommentEssentials.userEmail,
                url = editedCommentEssentials.userUrl
            )
        )
        if (result !is RsEditResult.Success) return false
        commentsStore
            .getCommentByLocalSiteAndRemoteId(site.id, commentIdentifier.remoteCommentId)
            .firstOrNull()?.let { cached ->
                val serverComment = cached.copy(
                    authorName = result.comment.authorName,
                    authorEmail = result.comment.authorEmail,
                    authorUrl = result.comment.authorUrl,
                    content = result.comment.contentRaw
                )
                // Local-only cache write (isError = false persists the entity without a network
                // round-trip).
                commentsStore.updateComment(isError = false, commentId = serverComment.id, comment = serverComment)
            }
        return true
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
                editedComment = editedComment,
                editErrorStrings = errors
            )
        }
    }

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
