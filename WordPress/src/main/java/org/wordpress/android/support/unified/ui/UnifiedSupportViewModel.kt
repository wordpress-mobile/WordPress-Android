package org.wordpress.android.support.unified.ui

import android.net.Uri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
import org.wordpress.android.support.unified.model.ConversationReplyFormState
import org.wordpress.android.support.unified.model.VideoDownloadState
import org.wordpress.android.support.unified.util.AttachmentStateValidator
import org.wordpress.android.support.unified.util.EncryptedAppLogsUploader
import org.wordpress.android.support.unified.util.TempAttachmentsUtil
import org.wordpress.android.support.unified.model.UnifiedConversation
import org.wordpress.android.support.unified.model.UnifiedMessage
import org.wordpress.android.support.unified.repository.UnifiedSupportRepository
import org.wordpress.android.ui.compose.utils.markdownToAnnotatedString
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import java.io.File
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class UnifiedSupportViewModel @Inject constructor(
    accountStore: AccountStore,
    private val repository: UnifiedSupportRepository,
    private val tempAttachmentsUtil: TempAttachmentsUtil,
    private val attachmentStateValidator: AttachmentStateValidator,
    private val encryptedAppLogsUploader: EncryptedAppLogsUploader,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    appLogWrapper: AppLogWrapper,
    networkUtilsWrapper: NetworkUtilsWrapper,
) : ConversationsSupportViewModel<UnifiedConversation>(accountStore, appLogWrapper, networkUtilsWrapper) {
    private val _isSendingReply = MutableStateFlow(false)
    val isSendingReply: StateFlow<Boolean> = _isSendingReply.asStateFlow()

    // Reply form state for HE-style conversations (survives configuration changes)
    private val _replyFormState = MutableStateFlow(ConversationReplyFormState())
    val replyFormState: StateFlow<ConversationReplyFormState> = _replyFormState.asStateFlow()

    // Cache for downloaded video file paths (videoUrl -> file path) used by the attachment player.
    // Concurrent because it is read/written from ioDispatcher download coroutines and cleared from
    // onCleared() on the main thread.
    private val videoCache = ConcurrentHashMap<String, String>()

    private val _videoDownloadState = MutableStateFlow<VideoDownloadState>(VideoDownloadState.Idle)
    val videoDownloadState: StateFlow<VideoDownloadState> = _videoDownloadState.asStateFlow()

    override fun initRepository(accessToken: String) {
        repository.init(accessToken, accountStore.account.userId)
    }

    fun getAuthorizationHeader(): String = "$BEARER_TAG ${accountStore.accessToken}"

    /**
     * Downloads a video attachment to a temporary file (with caching) so it can be played in-app.
     * Updates [videoDownloadState] as it progresses.
     */
    @Suppress("TooGenericExceptionCaught")
    fun downloadVideoToTempFile(videoUrl: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                videoCache[videoUrl]?.let { cachedFilePath ->
                    val cachedFile = File(cachedFilePath)
                    if (cachedFile.exists()) {
                        _videoDownloadState.value = VideoDownloadState.Success(cachedFile)
                        return@launch
                    } else {
                        videoCache.remove(videoUrl)
                    }
                }

                _videoDownloadState.value = VideoDownloadState.Downloading
                val tempFile = tempAttachmentsUtil.createVideoTempFile(videoUrl)
                if (tempFile == null) {
                    _videoDownloadState.value = VideoDownloadState.Error
                } else {
                    videoCache[videoUrl] = tempFile.absolutePath
                    _videoDownloadState.value = VideoDownloadState.Success(tempFile)
                }
            } catch (e: Exception) {
                appLogWrapper.e(AppLog.T.SUPPORT, "Error downloading video: ${e.stackTraceToString()}")
                _videoDownloadState.value = VideoDownloadState.Error
            }
        }
    }

    fun resetVideoDownloadState() {
        _videoDownloadState.value = VideoDownloadState.Idle
    }

    private fun cleanupVideoCache() {
        videoCache.values.forEach { filePath ->
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        videoCache.clear()
    }

    override fun onCleared() {
        super.onCleared()
        cleanupVideoCache()
    }

    /**
     * Starts a brand-new bot conversation. The conversation is created on the backend when the
     * first message is sent (see [sendReply]), mirroring the "Ask the Bots" flow.
     */
    fun onCreateNewBotConversationClick() {
        viewModelScope.launch {
            val now = Date()
            setNewConversation(
                UnifiedConversation(
                    id = NEW_CONVERSATION_ID,
                    title = "",
                    description = "",
                    status = UnifiedConversation.STATUS_BOT,
                    canAcceptReply = true,
                    createdAt = now,
                    updatedAt = now,
                    messages = emptyList()
                )
            )
        }
    }

    override suspend fun getConversations(): List<UnifiedConversation>? = repository.loadConversations()

    override suspend fun getConversation(conversationId: Long): UnifiedConversation? =
        repository.loadConversation(conversationId)

    @Suppress("TooGenericExceptionCaught", "LongMethod")
    fun sendReply(message: String, includeAppLogs: Boolean = false) {
        val conversation = _selectedConversation.value ?: return
        // Guard against concurrent sends. Setting the flag synchronously here (before launching the
        // coroutine) closes the race where two quick calls both pass the check before either coroutine
        // gets to set it.
        if (_isSendingReply.value) return
        _isSendingReply.value = true

        viewModelScope.launch {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                _errorMessage.value = ErrorType.OFFLINE
                _isSendingReply.value = false
                return@launch
            }

            val optimisticMessage = buildOptimisticUserMessage(message)
            val localMessages = conversation.messages + optimisticMessage
            _selectedConversation.value = conversation.copy(messages = localMessages)

            // Clear the input immediately so the message reads as sent while we wait for the
            // response. It is restored by rollbackInputMessage() if the send fails.
            _replyFormState.update { it.copy(message = "") }

            var tempAttachments: List<File> = emptyList()
            try {
                val isNewConversation = conversation.id == NEW_CONVERSATION_ID
                val updated = if (isNewConversation) {
                    // The create endpoint only returns the bot reply, so keep the local
                    // messages (including the optimistic question) like the Ask the Bots flow.
                    repository.createNewBotConversation(message)?.let { created ->
                        created.copy(messages = localMessages + created.messages)
                    }
                } else {
                    val encryptedLogIds = if (includeAppLogs) {
                        encryptedAppLogsUploader.uploadLogs()
                    } else {
                        emptyList()
                    }
                    val attachmentUris = _replyFormState.value.attachmentState.acceptedUris
                    tempAttachments = tempAttachmentsUtil.createTempFilesFrom(attachmentUris)
                    repository.replyToConversation(
                        conversationId = conversation.id,
                        message = message,
                        attachments = tempAttachments.map { it.path },
                        encryptedLogIds = encryptedLogIds
                    )
                }
                if (updated != null) {
                    _selectedConversation.value = updated
                    clearReplyForm()
                    if (isNewConversation) {
                        _conversations.value = listOf(updated.copy(messages = emptyList())) + _conversations.value
                    } else {
                        replaceInList(updated)
                    }
                } else {
                    rollbackOptimisticMessage(conversation, optimisticMessage.id)
                    rollbackInputMessage(message)
                    _errorMessage.value = ErrorType.GENERAL
                    appLogWrapper.e(AppLog.T.SUPPORT, "Error replying to unified conversation: response is null")
                }
            } catch (throwable: Throwable) {
                rollbackOptimisticMessage(conversation, optimisticMessage.id)
                rollbackInputMessage(message)
                _errorMessage.value = ErrorType.GENERAL
                appLogWrapper.e(
                    AppLog.T.SUPPORT,
                    "Error replying to unified conversation: ${throwable.message} - ${throwable.stackTraceToString()}"
                )
            } finally {
                tempAttachmentsUtil.removeTempFiles(tempAttachments)
                _isSendingReply.value = false
            }
        }
    }

    fun addReplyAttachments(uris: List<Uri>) {
        viewModelScope.launch(ioDispatcher) {
            // update {} applies the read-modify-write atomically so a concurrent removal or send-clear
            // running on another dispatcher can't be lost. The validator is pure, so re-running the
            // block on CAS contention is safe.
            _replyFormState.update { current ->
                val newState = attachmentStateValidator.addAttachments(current.attachmentState, uris)
                current.copy(attachmentState = newState)
            }
        }
    }

    fun removeReplyAttachment(uri: Uri) {
        viewModelScope.launch {
            _replyFormState.update { current ->
                val removedState = attachmentStateValidator.removeAttachment(current.attachmentState, uri)
                // Re-validate previously rejected uris against the space freed by the removal in the same
                // pass, so the form state is published exactly once (no transient intermediate emission).
                val revalidatedState =
                    attachmentStateValidator.addAttachments(removedState, removedState.rejectedUris)
                current.copy(attachmentState = revalidatedState)
            }
        }
    }

    fun updateReplyMessage(message: String) {
        _replyFormState.update { it.copy(message = message) }
    }

    fun updateReplyIncludeAppLogs(include: Boolean) {
        _replyFormState.update { it.copy(includeAppLogs = include) }
    }

    fun updateReplyBottomSheetVisibility(isVisible: Boolean) {
        _replyFormState.update { it.copy(isBottomSheetVisible = isVisible) }
    }

    fun clearReplyForm() {
        _replyFormState.value = ConversationReplyFormState()
    }

    // Clear any draft reply (text + attachments) when opening a conversation, so a draft started in
    // one conversation never leaks into another regardless of how the previous one was exited.
    override fun onConversationOpened() {
        clearReplyForm()
    }

    fun notifyGeneralError() {
        _errorMessage.value = ErrorType.GENERAL
    }

    private fun buildOptimisticUserMessage(message: String): UnifiedMessage =
        UnifiedMessage(
            id = -System.currentTimeMillis(),
            formattedText = markdownToAnnotatedString(message),
            authorRole = UnifiedMessage.AUTHOR_ROLE_USER,
            authorName = _userInfo.value.userName,
            createdAt = Date(),
            attachments = emptyList()
        )

    private fun rollbackInputMessage(message: String) {
        // Restore the unsent text into the input, but only if the user hasn't started typing a
        // new message while the send was in flight (so we don't clobber their newer text).
        _replyFormState.update { current ->
            if (current.message.isEmpty()) current.copy(message = message) else current
        }
    }

    private fun rollbackOptimisticMessage(original: UnifiedConversation, optimisticId: Long) {
        val current = _selectedConversation.value ?: return
        if (current.id != original.id) return
        _selectedConversation.value = current.copy(
            messages = current.messages.filterNot { it.id == optimisticId }
        )
    }

    private fun replaceInList(updated: UnifiedConversation) {
        _conversations.value = _conversations.value.map { existing ->
            if (existing.id == updated.id) updated.copy(messages = emptyList()) else existing
        }
    }

    companion object {
        private const val NEW_CONVERSATION_ID = 0L
        private const val BEARER_TAG = "Bearer"
    }
}
