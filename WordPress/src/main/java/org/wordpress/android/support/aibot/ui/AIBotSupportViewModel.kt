package org.wordpress.android.support.aibot.ui

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
import org.wordpress.android.util.AppLog
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AIBotSupportViewModel @Inject constructor(
    accountStore: AccountStore,
    private val aiBotSupportRepository: AIBotSupportRepository,
    appLogWrapper: AppLogWrapper,
) : ConversationsSupportViewModel<BotConversation>(accountStore, appLogWrapper) {
    private val _canSendMessage = MutableStateFlow(true)
    val canSendMessage: StateFlow<Boolean> = _canSendMessage.asStateFlow()

    private val _isBotTyping = MutableStateFlow(false)
    val isBotTyping: StateFlow<Boolean> = _isBotTyping.asStateFlow()

    override fun initRepository(accessToken: String) {
        aiBotSupportRepository.init(accessToken, accountStore.account.userId)
    }

    override suspend fun getConversations() = aiBotSupportRepository.loadConversations()

    override suspend fun getConversation(conversationId: Long): BotConversation? {
        _canSendMessage.value = false
        return aiBotSupportRepository.loadConversation(conversationId).also {
            _canSendMessage.value = true
        }
    }

    fun onNewConversationClick() {
        viewModelScope.launch {
            val now = Date()
            val botConversation = BotConversation(
                id = 0,
                createdAt = now,
                mostRecentMessageDate = now,
                lastMessage = "",
                messages = listOf()
            )
            _canSendMessage.value = true
            setNewConversation(botConversation)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun sendMessage(message: String) {
        viewModelScope.launch {
            try {
                // Show bot typing indicator and limit send messages
                _isBotTyping.value = true
                _canSendMessage.value = false

                val now = Date()
                val userMessage = BotMessage(
                    id = System.currentTimeMillis(),
                    text = message,
                    date = now,
                    isWrittenByUser = true
                )
                val currentMessages = (_selectedConversation.value?.messages ?: emptyList()) + userMessage
                _selectedConversation.value = _selectedConversation.value?.copy(
                    messages = currentMessages
                )

                val conversation = sendMessageToBot(message)

                // Hide bot typing indicator
                _isBotTyping.value = false

                if (conversation != null) {
                    val finalConversation = conversation.copy(
                        lastMessage = conversation.messages.last().text,
                        messages = (_selectedConversation.value?.messages ?: emptyList()) + conversation.messages
                    )
                    // Update the conversations list
                    val currentConversations =_conversations.value
                    if (currentConversations.none { it.id == conversation.id }) {
                        // It's a new conversation, so add it to the top
                        _conversations.value = listOf(conversation) + _conversations.value
                    } else {
                        // The conversation exists, so we modify it
                        _conversations.value = _conversations.value.map {
                            if (it.id == conversation.id) {
                                finalConversation
                            } else {
                                it
                            }
                        }
                    }

                    // Update the selected conversation
                    _selectedConversation.value = finalConversation
                } else {
                    _errorMessage.value = ErrorType.GENERAL
                    appLogWrapper.e(AppLog.T.SUPPORT, "Error sending message: response is null")
                }
            } catch (throwable: Throwable) {
                _errorMessage.value = ErrorType.GENERAL
                _isBotTyping.value = false
                appLogWrapper.e(AppLog.T.SUPPORT, "Error sending message: " +
                        "${throwable.message} - ${throwable.stackTraceToString()}")
            }

            // Be sure we allow the user to send messages again
            _canSendMessage.value = true
        }
    }

    private suspend fun sendMessageToBot(message: String): BotConversation? {
        val conversationId = _selectedConversation.value?.id ?: 0L
        return if (conversationId == 0L) {
            // This is a new conversation, so we need to create it first
            aiBotSupportRepository.createNewConversation(message)
        } else {
            aiBotSupportRepository.sendMessageToConversation(conversationId, message)
        }
    }
}
