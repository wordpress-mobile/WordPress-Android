package org.wordpress.android.support.unified.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
import org.wordpress.android.support.unified.model.UnifiedConversation
import org.wordpress.android.support.unified.model.UnifiedMessage
import org.wordpress.android.support.unified.repository.UnifiedSupportRepository
import org.wordpress.android.ui.compose.utils.markdownToAnnotatedString
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class UnifiedSupportViewModel @Inject constructor(
    accountStore: AccountStore,
    private val repository: UnifiedSupportRepository,
    appLogWrapper: AppLogWrapper,
    networkUtilsWrapper: NetworkUtilsWrapper,
) : ConversationsSupportViewModel<UnifiedConversation>(accountStore, appLogWrapper, networkUtilsWrapper) {
    private val _isSendingReply = MutableStateFlow(false)
    val isSendingReply: StateFlow<Boolean> = _isSendingReply.asStateFlow()

    override fun initRepository(accessToken: String) {
        repository.init(accessToken)
    }

    override suspend fun getConversations(): List<UnifiedConversation> = repository.loadConversations()

    override suspend fun getConversation(conversationId: Long): UnifiedConversation? =
        repository.loadConversation(conversationId)

    @Suppress("TooGenericExceptionCaught")
    fun sendReply(message: String) {
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

            try {
                val updated = repository.replyToConversation(conversation.id, message)
                if (updated != null) {
                    _selectedConversation.value = updated
                    replaceInList(updated)
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
                _isSendingReply.value = false
            }
        }
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
}
