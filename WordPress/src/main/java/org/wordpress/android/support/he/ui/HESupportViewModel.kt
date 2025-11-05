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
import org.wordpress.android.support.he.model.MessageSendResult
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.repository.CreateConversationResult
import org.wordpress.android.support.he.repository.HESupportRepository
import org.wordpress.android.support.he.util.TempAttachmentsUtil
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
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
    }
    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private val _messageSendResult = MutableStateFlow<MessageSendResult?>(null)
    val messageSendResult: StateFlow<MessageSendResult?> = _messageSendResult.asStateFlow()

    // Unified attachment state (shared for both Detail and NewTicket screens)
    private val _attachmentState = MutableStateFlow(AttachmentState())
    val attachmentState: StateFlow<AttachmentState> = _attachmentState.asStateFlow()

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
                _isSendingMessage.value = true

                val files = tempAttachmentsUtil.createTempFilesFrom(_attachmentState.value.acceptedUris)

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
                        // Clear attachments after successful creation
                        _attachmentState.value = AttachmentState()
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
                val selectedConversation = _selectedConversation.value
                if (selectedConversation == null) {
                    appLogWrapper.e(AppLog.T.SUPPORT, "Error answering a conversation: no conversation selected")
                    return@launch
                }

                _isSendingMessage.value = true
                val files = tempAttachmentsUtil.createTempFilesFrom(_attachmentState.value.acceptedUris)

                when (val result = heSupportRepository.addMessageToConversation(
                    conversationId = selectedConversation.id,
                    message = message,
                    attachments = files.map { it.path }
                )) {
                    is CreateConversationResult.Success -> {
                        _selectedConversation.value = result.conversation
                        _messageSendResult.value = MessageSendResult.Success
                        // Clear attachments after successful message send
                        _attachmentState.value = AttachmentState()
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

    fun addAttachments(uris: List<Uri>) {
        viewModelScope.launch(ioDispatcher) {
            _attachmentState.value = validateAndCreateAttachmentState(uris)
        }
    }

    @Suppress("LoopWithTooManyJumpStatements")
    private suspend fun validateAndCreateAttachmentState(uris: List<Uri>): AttachmentState = withContext(ioDispatcher) {
        val validUris = mutableListOf<Uri>()
        val skippedUris = mutableListOf<Uri>()
        var skippedDueToFileSize = false
        var skippedDueToTotalSize = false

        // Calculate current total size
        var currentTotalSize = 0L
        for (uri in _attachmentState.value.acceptedUris) {
            val fileSize = getFileSize(uri) ?: 0L
            currentTotalSize += fileSize
        }

        // Validate each new attachment
        for (uri in uris) {
            val fileSize = getFileSize(uri)

            // Skip if we can't determine file size we just allow it to be added
            if (fileSize != null) {
                // Check individual file size
                if (fileSize > MAX_TOTAL_SIZE_BYTES) {
                    skippedDueToFileSize = true
                    skippedUris.add(uri)
                    continue
                }

                // Check if adding this file would exceed total size limit
                if (currentTotalSize + fileSize > MAX_TOTAL_SIZE_BYTES) {
                    skippedDueToTotalSize = true
                    skippedUris.add(uri)
                    continue
                }
            }

            // File is valid, add it
            validUris.add(uri)
            currentTotalSize += fileSize ?: 0
        }

        // Build the new attachment state
        val currentAccepted = _attachmentState.value.acceptedUris
        val newAccepted = currentAccepted + validUris

        // Determine rejection reason - prioritize TotalSizeExceeded as it refers to the whole request
        val rejectionReason = when {
            skippedDueToTotalSize -> AttachmentState.RejectionReason.TotalSizeExceeded
            skippedDueToFileSize -> AttachmentState.RejectionReason.FileTooLarge
            else -> null
        }

        AttachmentState(
            acceptedUris = newAccepted,
            rejectedUris = if (rejectionReason != null) skippedUris else emptyList(),
            rejectionReason = rejectionReason
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

    fun removeAttachment(uri: Uri) {
        viewModelScope.launch {
            val currentState = _attachmentState.value
            _attachmentState.value = currentState.copy(
                acceptedUris = currentState.acceptedUris.filter { it != uri },
            )
        }
    }

    fun clearAttachments() {
        _attachmentState.value = AttachmentState()
    }

    fun notifyGeneralError() {
        _errorMessage.value = ErrorType.GENERAL
    }
}
