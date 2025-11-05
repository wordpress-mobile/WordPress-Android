package org.wordpress.android.support.he.ui

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
    accountStore: AccountStore,
    appLogWrapper: AppLogWrapper,
    networkUtilsWrapper: NetworkUtilsWrapper,
) : ConversationsSupportViewModel<SupportConversation>(accountStore, appLogWrapper, networkUtilsWrapper) {
    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private val _messageSendResult = MutableStateFlow<MessageSendResult?>(null)
    val messageSendResult: StateFlow<MessageSendResult?> = _messageSendResult.asStateFlow()

    // Attachment state (shared for both Detail and NewTicket screens)
    private val _attachments = MutableStateFlow<List<Uri>>(emptyList())
    val attachments: StateFlow<List<Uri>> = _attachments.asStateFlow()

    sealed class MessageSendResult {
        data object Success : MessageSendResult()
        data object Failure : MessageSendResult()
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

                val files = tempAttachmentsUtil.createTempFilesFrom(_attachments.value)

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
                        _attachments.value = emptyList()
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
                val files = tempAttachmentsUtil.createTempFilesFrom(_attachments.value)

                when (val result = heSupportRepository.addMessageToConversation(
                    conversationId = selectedConversation.id,
                    message = message,
                    attachments = files.map { it.path }
                )) {
                    is CreateConversationResult.Success -> {
                        _selectedConversation.value = result.conversation
                        _messageSendResult.value = MessageSendResult.Success
                        // Clear attachments after successful message send
                        _attachments.value = emptyList()
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
        _attachments.value = _attachments.value + uris
    }

    fun removeAttachment(uri: Uri) {
        _attachments.value = _attachments.value.filter { it != uri }
    }

    fun clearAttachments() {
        _attachments.value = emptyList()
    }

    fun notifyGeneralError() {
        _errorMessage.value = ErrorType.GENERAL
    }
}
