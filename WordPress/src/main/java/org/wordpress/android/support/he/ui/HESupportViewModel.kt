package org.wordpress.android.support.he.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
import org.wordpress.android.support.he.model.AttachmentState
import org.wordpress.android.support.he.model.ConversationReplyFormState
import org.wordpress.android.support.he.model.MessageSendResult
import org.wordpress.android.support.he.model.NewTicketFormState
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.model.VideoDownloadState
import org.wordpress.android.support.he.repository.CreateConversationResult
import org.wordpress.android.support.he.repository.HESupportRepository
import org.wordpress.android.support.he.util.TempAttachmentsUtil
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import java.io.File
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class HESupportViewModel @Inject constructor(
    private val heSupportRepository: HESupportRepository,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val tempAttachmentsUtil: TempAttachmentsUtil,
    private val application: Application,
    accountStore: AccountStore,
    appLogWrapper: AppLogWrapper,
    networkUtilsWrapper: NetworkUtilsWrapper,
) : ConversationsSupportViewModel<SupportConversation>(accountStore, appLogWrapper, networkUtilsWrapper) {
    companion object {
        const val MAX_TOTAL_SIZE_BYTES = 20L * 1024 * 1024 // 20MB total
        private const val BEARER_TAG = "Bearer"
    }
    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private val _messageSendResult = MutableStateFlow<MessageSendResult?>(null)
    val messageSendResult: StateFlow<MessageSendResult?> = _messageSendResult.asStateFlow()

    // Cache for downloaded video file paths (videoUrl -> file path)
    // Stores paths instead of File objects to minimize memory footprint
    private val videoCache = mutableMapOf<String, String>()

    // Video download state
    private val _videoDownloadState = MutableStateFlow<VideoDownloadState>(VideoDownloadState.Idle)
    val videoDownloadState: StateFlow<VideoDownloadState> = _videoDownloadState.asStateFlow()

    // New ticket form state (survives configuration changes)
    private val _newTicketFormState = MutableStateFlow(NewTicketFormState())
    val newTicketFormState: StateFlow<NewTicketFormState> = _newTicketFormState.asStateFlow()

    // Conversation reply form state (survives configuration changes)
    private val _replyFormState = MutableStateFlow(ConversationReplyFormState())
    val replyFormState: StateFlow<ConversationReplyFormState> = _replyFormState.asStateFlow()

    override fun initRepository(accessToken: String) {
        heSupportRepository.init(accessToken)
    }

    override suspend fun getConversations(): List<SupportConversation> = heSupportRepository.loadConversations()

    @Suppress("TooGenericExceptionCaught")
    fun onSendNewConversation(
        subject: String,
        message: String,
        tags: List<String>,
    ) {
        viewModelScope.launch(ioDispatcher) {
            try {
                if (!networkUtilsWrapper.isNetworkAvailable()) {
                    _errorMessage.value = ErrorType.OFFLINE
                    return@launch
                }

                _isSendingMessage.value = true

                val attachmentUris = _newTicketFormState.value.attachmentState.acceptedUris
                val files = tempAttachmentsUtil.createTempFilesFrom(attachmentUris)

                when (val result = heSupportRepository.createConversation(
                    subject = subject,
                    message = message,
                    tags = tags,
                    attachments = files.map { it.path }
                )) {
                    is CreateConversationResult.Success -> {
                        val newConversation = result.conversation
                        // update conversations locally
                        _conversations.value = listOf(newConversation) + _conversations.value
                        // Clear form state after successful creation
                        clearNewTicketForm()
                        onBackClick()
                    }

                    is CreateConversationResult.Error.Forbidden -> {
                        _errorMessage.value = ErrorType.FORBIDDEN
                        appLogWrapper.e(AppLog.T.SUPPORT, "Unauthorized error creating HE conversation")
                    }

                    is CreateConversationResult.Error.GeneralError -> {
                        _errorMessage.value = ErrorType.GENERAL
                        appLogWrapper.e(AppLog.T.SUPPORT, "General error creating HE conversation")
                    }
                }

                tempAttachmentsUtil.removeTempFiles(files)
                _isSendingMessage.value = false
            } catch (e: Exception) {
                _errorMessage.value = ErrorType.GENERAL
                appLogWrapper.e(
                    AppLog.T.SUPPORT,
                    "Error creating HE conversation ${e.stackTraceToString()}"
                )
            }
        }
    }

    override suspend fun getConversation(conversationId: Long): SupportConversation? =
        heSupportRepository.loadConversation(conversationId)

    @Suppress("TooGenericExceptionCaught")
    fun onAddMessageToConversation(message: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                if (!networkUtilsWrapper.isNetworkAvailable()) {
                    _messageSendResult.value = MessageSendResult.Failure
                    _errorMessage.value = ErrorType.OFFLINE
                    return@launch
                }

                val selectedConversation = _selectedConversation.value
                if (selectedConversation == null) {
                    appLogWrapper.e(AppLog.T.SUPPORT, "Error answering a conversation: no conversation selected")
                    return@launch
                }

                _isSendingMessage.value = true
                val attachmentUris = _replyFormState.value.attachmentState.acceptedUris
                val files = tempAttachmentsUtil.createTempFilesFrom(attachmentUris)

                when (val result = heSupportRepository.addMessageToConversation(
                    conversationId = selectedConversation.id,
                    message = message,
                    attachments = files.map { it.path }
                )) {
                    is CreateConversationResult.Success -> {
                        _selectedConversation.value = result.conversation
                        _messageSendResult.value = MessageSendResult.Success
                        // Clear reply form after successful message send
                        clearReplyForm()
                    }

                    is CreateConversationResult.Error.Forbidden -> {
                        _errorMessage.value = ErrorType.FORBIDDEN
                        appLogWrapper.e(AppLog.T.SUPPORT, "Unauthorized error adding message to HE conversation")
                        _messageSendResult.value = MessageSendResult.Failure
                    }

                    is CreateConversationResult.Error.GeneralError -> {
                        _errorMessage.value = ErrorType.GENERAL
                        appLogWrapper.e(AppLog.T.SUPPORT, "General error adding message to HE conversation")
                        _messageSendResult.value = MessageSendResult.Failure
                    }
                }

                tempAttachmentsUtil.removeTempFiles(files)
                _isSendingMessage.value = false
            } catch (e: Exception) {
                _errorMessage.value = ErrorType.GENERAL
                appLogWrapper.e(
                    AppLog.T.SUPPORT,
                    "Error adding message to HE conversation: ${e.stackTraceToString()}"
                )
            }
        }
    }

    fun clearMessageSendResult() {
        _messageSendResult.value = null
    }

    fun addNewTicketAttachments(uris: List<Uri>) {
        viewModelScope.launch(ioDispatcher) {
            val currentState = _newTicketFormState.value.attachmentState
            val newState = validateAndCreateAttachmentState(currentState, uris)
            _newTicketFormState.value = _newTicketFormState.value.copy(attachmentState = newState)
        }
    }

    fun removeNewTicketAttachment(uri: Uri) {
        viewModelScope.launch {
            val currentState = _newTicketFormState.value.attachmentState
            val updatedState = removeAttachmentFromState(currentState, uri)
            _newTicketFormState.value = _newTicketFormState.value.copy(attachmentState = updatedState)
            addNewTicketAttachments(currentState.rejectedUris)
        }
    }

    fun addReplyAttachments(uris: List<Uri>) {
        viewModelScope.launch(ioDispatcher) {
            val currentState = _replyFormState.value.attachmentState
            val newState = validateAndCreateAttachmentState(currentState, uris)
            _replyFormState.value = _replyFormState.value.copy(attachmentState = newState)
        }
    }

    fun removeReplyAttachment(uri: Uri) {
        viewModelScope.launch {
            val currentState = _replyFormState.value.attachmentState
            val updatedState = removeAttachmentFromState(currentState, uri)
            _replyFormState.value = _replyFormState.value.copy(attachmentState = updatedState)
            addReplyAttachments(currentState.rejectedUris)
        }
    }

    private fun removeAttachmentFromState(currentState: AttachmentState, uri: Uri): AttachmentState {
        val newAcceptedUris = currentState.acceptedUris.filter { it != uri }
        return currentState.copy(acceptedUris = newAcceptedUris)
    }

    @Suppress("LoopWithTooManyJumpStatements")
    private suspend fun validateAndCreateAttachmentState(
        currentAttachmentState: AttachmentState,
        uris: List<Uri>
    ): AttachmentState = withContext(ioDispatcher) {
        if (uris.isEmpty()) {
            return@withContext currentAttachmentState
        }

        val validUris = mutableListOf<Uri>()
        val skippedUris = mutableListOf<Uri>()

        // Calculate current total size
        var currentTotalSize = calculateTotalSize(currentAttachmentState.acceptedUris)

        // Validate each new attachment
        for (uri in uris) {
            val fileSize = getFileSize(uri)

            // Skip if we can't determine file size we just allow it to be added
            if (fileSize != null) {
                // Check if adding this file would exceed total size limit
                if (currentTotalSize + fileSize > MAX_TOTAL_SIZE_BYTES) {
                    skippedUris.add(uri)
                    continue
                }
            }

            // File is valid, add it
            validUris.add(uri)
            currentTotalSize += fileSize ?: 0
        }

        // Build the new attachment state
        val currentAccepted = currentAttachmentState.acceptedUris
        val newAccepted = currentAccepted + validUris

        // Calculate rejected total size
        val rejectedTotalSize = calculateTotalSize(skippedUris)

        AttachmentState(
            acceptedUris = newAccepted,
            rejectedUris = skippedUris,
            currentTotalSizeBytes = currentTotalSize,
            rejectedTotalSizeBytes = rejectedTotalSize
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun getFileSize(uri: Uri): Long? = withContext(ioDispatcher) {
        try {
            application.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length
            }
        } catch (e: Exception) {
            appLogWrapper.d(AppLog.T.SUPPORT, "Could not determine file size for URI: $uri - ${e.message}")
            // Silently return null if we can't get the file size
            // This will be handled by the validation logic
            null
        }
    }

    /**
     * Calculates the total size of all files in the list
     * @param uris List of URIs to calculate size for
     * @return Total size in bytes
     */
    private suspend fun calculateTotalSize(uris: List<Uri>): Long {
        var totalSize = 0L
        for (uri in uris) {
            totalSize += getFileSize(uri) ?: 0L
        }
        return totalSize
    }

    fun updateNewTicketCategory(category: SupportCategory?) {
        _newTicketFormState.value = _newTicketFormState.value.copy(category = category)
    }

    fun updateNewTicketSubject(subject: String) {
        _newTicketFormState.value = _newTicketFormState.value.copy(subject = subject)
    }

    fun updateNewTicketSiteAddress(siteAddress: String) {
        _newTicketFormState.value = _newTicketFormState.value.copy(siteAddress = siteAddress)
    }

    fun updateNewTicketMessage(message: String) {
        _newTicketFormState.value = _newTicketFormState.value.copy(message = message)
    }

    fun updateNewTicketIncludeAppLogs(include: Boolean) {
        _newTicketFormState.value = _newTicketFormState.value.copy(includeAppLogs = include)
    }

    fun clearNewTicketForm() {
        _newTicketFormState.value = NewTicketFormState()
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

    /**
     * Downloads a video to a temporary file with caching and state management.
     * Updates videoDownloadState as it progresses.
     */
    @Suppress("TooGenericExceptionCaught")
    fun downloadVideoToTempFile(videoUrl: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                // Check cache first (before setting state to avoid unnecessary state changes)
                videoCache[videoUrl]?.let { cachedFilePath ->
                    val cachedFile = File(cachedFilePath)
                    if (cachedFile.exists()) {
                        AppLog.d(AppLog.T.SUPPORT, "Using cached video file for: $videoUrl")
                        _videoDownloadState.value = VideoDownloadState.Success(cachedFile)
                        return@launch
                    } else {
                        // File was deleted, remove from cache
                        videoCache.remove(videoUrl)
                    }
                }

                // Start downloading
                _videoDownloadState.value = VideoDownloadState.Downloading
                AppLog.d(AppLog.T.SUPPORT, "Downloading video to temp file: $videoUrl")
                val tempFile = tempAttachmentsUtil.createVideoTempFile(videoUrl)
                if (tempFile == null) {
                    _videoDownloadState.value = VideoDownloadState.Error
                } else {
                    // Cache the downloaded file path
                    videoCache[videoUrl] = tempFile.absolutePath
                    _videoDownloadState.value = VideoDownloadState.Success(tempFile)
                }
            } catch (e: Exception) {
                AppLog.e(AppLog.T.SUPPORT, "Error downloading video", e)
                _videoDownloadState.value = VideoDownloadState.Error
            }
        }
    }

    /**
     * Resets the video download state to Idle. Call this when closing the video player.
     */
    fun resetVideoDownloadState() {
        _videoDownloadState.value = VideoDownloadState.Idle
    }

    /**
     * Cleans up all cached video files. Call this when the activity is destroyed.
     */
    fun cleanupVideoCache() {
        AppLog.d(AppLog.T.SUPPORT, "Cleaning up ${videoCache.size} cached video files")
        videoCache.values.forEach { filePath ->
            val file = File(filePath)
            if (file.exists()) {
                AppLog.d(AppLog.T.SUPPORT, "Deleting temp video file: $filePath")
                file.delete()
            }
        }
        videoCache.clear()
    }

    fun getAuthorizationHeader():String = "$BEARER_TAG ${accountStore.accessToken}"

    /**
     * Called when the ViewModel is destroyed. Ensures video cache cleanup even if Activity
     * onDestroy() is not called (e.g., process death).
     */
    override fun onCleared() {
        super.onCleared()
        cleanupVideoCache()
    }
}
