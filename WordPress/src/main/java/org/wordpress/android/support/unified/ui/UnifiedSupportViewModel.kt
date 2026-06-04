package org.wordpress.android.support.unified.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.aibot.model.BotConversation
import org.wordpress.android.support.aibot.model.BotMessage
import org.wordpress.android.support.aibot.repository.AIBotSupportRepository
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
    private val aiBotSupportRepository: AIBotSupportRepository,
    appLogWrapper: AppLogWrapper,
    networkUtilsWrapper: NetworkUtilsWrapper,
) : ConversationsSupportViewModel<UnifiedConversation>(accountStore, appLogWrapper, networkUtilsWrapper) {
    private val _isSendingReply = MutableStateFlow(false)
    val isSendingReply: StateFlow<Boolean> = _isSendingReply.asStateFlow()

    override fun initRepository(accessToken: String) {
        repository.init(accessToken)
        // New conversations are created as bot chats through the AI bot endpoint.
        aiBotSupportRepository.init(accessToken, accountStore.account.userId)
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
                val isNewConversation = conversation.id == NEW_CONVERSATION_ID
                val updated = if (isNewConversation) {
                    aiBotSupportRepository.createNewConversation(message)?.toUnifiedConversation()
                } else {
                    repository.replyToConversation(conversation.id, message)
                }
                if (updated != null) {
                    _selectedConversation.value = updated
                    if (isNewConversation) {
                        _conversations.value = listOf(updated) + _conversations.value
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
    }
}
