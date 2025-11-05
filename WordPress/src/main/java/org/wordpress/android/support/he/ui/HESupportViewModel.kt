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
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
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
        const val MAX_ATTACHMENT_SIZE_BYTES = 20L * 1024 * 1024 // 20MB per file
        const val MAX_TOTAL_SIZE_BYTES = 40L * 1024 * 1024 // 40MB total
    }
    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private val _messageSendResult = MutableStateFlow<MessageSendResult?>(null)
    val messageSendResult: StateFlow<MessageSendResult?> = _messageSendResult.asStateFlow()

    // Unified attachment state (shared for both Detail and NewTicket screens)
    private val _attachmentState = MutableStateFlow(AttachmentState())
    val attachmentState: StateFlow<AttachmentState> = _attachmentState.asStateFlow()

    sealed class MessageSendResult {
        data object Success : MessageSendResult()
        data object Failure : MessageSendResult()
    }

    data class AttachmentState(
        val acceptedUris: List<Uri> = emptyList(),
        val rejectedUris: List<Uri> = emptyList(),
        val rejectionReason: RejectionReason? = null
    )

    sealed class RejectionReason {
        data object FileTooLarge : RejectionReason()
        data object TotalSizeExceeded : RejectionReason()
    }

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

    private fun validateAndCreateAttachmentState(uris: List<Uri>): AttachmentState {
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

            // Skip if we can't determine file size
            if (fileSize == null) {
                skippedUris.add(uri)
                continue
            }

            // Check individual file size
            if (fileSize > MAX_ATTACHMENT_SIZE_BYTES) {
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

            // File is valid, add it
            validUris.add(uri)
            currentTotalSize += fileSize
        }

        // Build the new attachment state
        val currentAccepted = _attachmentState.value.acceptedUris
        val newAccepted = currentAccepted + validUris

        return when {
            skippedDueToFileSize -> {
                AttachmentState(
                    acceptedUris = newAccepted,
                    rejectedUris = skippedUris,
                    rejectionReason = RejectionReason.FileTooLarge
                )
            }
            skippedDueToTotalSize -> {
                AttachmentState(
                    acceptedUris = newAccepted,
                    rejectedUris = skippedUris,
                    rejectionReason = RejectionReason.TotalSizeExceeded
                )
            }
            else -> {
                AttachmentState(
                    acceptedUris = newAccepted,
                    rejectedUris = emptyList(),
                    rejectionReason = null
                )
            }
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun getFileSize(uri: Uri): Long? {
        return try {
            application.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length
            }
        } catch (_: Exception) {
            // Silently return null if we can't get the file size
            // This will be handled by the validation logic
            null
        }
    }

    fun removeAttachment(uri: Uri) {
        val currentState = _attachmentState.value
        _attachmentState.value = currentState.copy(
            acceptedUris = currentState.acceptedUris.filter { it != uri },
            rejectedUris = emptyList(),
            rejectionReason = null
        )
    }

    fun clearAttachments() {
        _attachmentState.value = AttachmentState()
    }

    fun notifyGeneralError() {
        _errorMessage.value = ErrorType.GENERAL
    }
}
