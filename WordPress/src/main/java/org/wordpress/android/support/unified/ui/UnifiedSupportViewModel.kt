package org.wordpress.android.support.unified.ui

import android.net.Uri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.support.aibot.model.BotConversation
import org.wordpress.android.support.aibot.model.BotMessage
import org.wordpress.android.support.aibot.repository.AIBotSupportRepository
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
import org.wordpress.android.support.he.model.ConversationReplyFormState
import org.wordpress.android.support.he.model.VideoDownloadState
import org.wordpress.android.support.he.util.AttachmentStateValidator
import org.wordpress.android.support.he.util.EncryptedAppLogsUploader
import org.wordpress.android.support.he.util.TempAttachmentsUtil
import org.wordpress.android.support.unified.model.UnifiedConversation
import org.wordpress.android.support.unified.model.UnifiedMessage
import org.wordpress.android.support.unified.repository.UnifiedSupportRepository
import org.wordpress.android.ui.compose.utils.markdownToAnnotatedString
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class UnifiedSupportViewModel @Inject constructor(
    accountStore: AccountStore,
    private val repository: UnifiedSupportRepository,
    private val aiBotSupportRepository: AIBotSupportRepository,
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
    private val videoCache = mutableMapOf<String, String>()

    private val _videoDownloadState = MutableStateFlow<VideoDownloadState>(VideoDownloadState.Idle)
    val videoDownloadState: StateFlow<VideoDownloadState> = _videoDownloadState.asStateFlow()

    override fun initRepository(accessToken: String) {
        repository.init(accessToken)
        // New conversations are created as bot chats through the AI bot endpoint.
        aiBotSupportRepository.init(accessToken, accountStore.account.userId)
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

    fun cleanupVideoCache() {
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

    override suspend fun getConversations(): List<UnifiedConversation> = repository.loadConversations()

    override suspend fun getConversation(conversationId: Long): UnifiedConversation? =
        repository.loadConversation(conversationId)

    @Suppress("TooGenericExceptionCaught", "LongMethod")
    fun sendReply(message: String, includeAppLogs: Boolean = false) {
        val conversation = _selectedConversation.value ?: return
        if (_isSendingReply.value) return

        viewModelScope.launch {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                _errorMessage.value = ErrorType.OFFLINE
                return@launch
            }

            _isSendingReply.value = true
            val optimisticMessage = buildOptimisticUserMessage(message)
            _selectedConversation.value = conversation.copy(
                messages = conversation.messages + optimisticMessage
            )

            var tempAttachments: List<File> = emptyList()
            try {
                val isNewConversation = conversation.id == NEW_CONVERSATION_ID
                val updated = if (isNewConversation) {
                    // The create endpoint only returns the bot reply, so keep the local
                    // messages (including the optimistic question) like the Ask the Bots flow.
                    aiBotSupportRepository.createNewConversation(message)?.toUnifiedConversation()?.let { created ->
                        val localMessages = _selectedConversation.value?.messages ?: emptyList()
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
                    _errorMessage.value = ErrorType.GENERAL
                    appLogWrapper.e(AppLog.T.SUPPORT, "Error replying to unified conversation: response is null")
                }
            } catch (throwable: Throwable) {
                rollbackOptimisticMessage(conversation, optimisticMessage.id)
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
            val currentState = _replyFormState.value.attachmentState
            val newState = attachmentStateValidator.addAttachments(currentState, uris)
            _replyFormState.value = _replyFormState.value.copy(attachmentState = newState)
        }
    }

    fun removeReplyAttachment(uri: Uri) {
        viewModelScope.launch {
            val currentState = _replyFormState.value.attachmentState
            val updatedState = attachmentStateValidator.removeAttachment(currentState, uri)
            _replyFormState.value = _replyFormState.value.copy(attachmentState = updatedState)
            addReplyAttachments(currentState.rejectedUris)
        }
    }

    fun updateReplyMessage(message: String) {
        _replyFormState.value = _replyFormState.value.copy(message = message)
    }

    fun updateReplyIncludeAppLogs(include: Boolean) {
        _replyFormState.value = _replyFormState.value.copy(includeAppLogs = include)
    }

    fun updateReplyBottomSheetVisibility(isVisible: Boolean) {
        _replyFormState.value = _replyFormState.value.copy(isBottomSheetVisible = isVisible)
    }

    fun clearReplyForm() {
        _replyFormState.value = ConversationReplyFormState()
    }

    fun notifyGeneralError() {
        _errorMessage.value = ErrorType.GENERAL
    }

    private fun buildOptimisticUserMessage(message: String): UnifiedMessage =
        UnifiedMessage(
            id = -System.currentTimeMillis(),
            rawText = message,
            formattedText = markdownToAnnotatedString(message),
            authorRole = UnifiedMessage.AUTHOR_ROLE_USER,
            authorName = _userInfo.value.userName,
            createdAt = Date(),
            attachments = emptyList()
        )

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

    private fun BotConversation.toUnifiedConversation(): UnifiedConversation =
        UnifiedConversation(
            id = id,
            title = "",
            description = lastMessage,
            status = UnifiedConversation.STATUS_BOT,
            canAcceptReply = true,
            createdAt = createdAt,
            updatedAt = mostRecentMessageDate,
            messages = messages.map { it.toUnifiedMessage() }
        )

    private fun BotMessage.toUnifiedMessage(): UnifiedMessage =
        UnifiedMessage(
            id = id,
            rawText = rawText,
            formattedText = formattedText,
            authorRole = if (isWrittenByUser) UnifiedMessage.AUTHOR_ROLE_USER else UnifiedMessage.AUTHOR_ROLE_BOT,
            authorName = "",
            createdAt = date,
            attachments = emptyList()
        )

    companion object {
        private const val NEW_CONVERSATION_ID = 0L
        private const val BEARER_TAG = "Bearer"
    }
}
