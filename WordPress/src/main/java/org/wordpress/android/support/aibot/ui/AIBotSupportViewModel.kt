package org.wordpress.android.support.aibot.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.support.aibot.model.BotConversation
import org.wordpress.android.support.aibot.model.BotMessage
import org.wordpress.android.support.aibot.repository.AIBotSupportRepository
import java.util.Date
import javax.inject.Inject
import kotlin.Long

@HiltViewModel
class AIBotSupportViewModel @Inject constructor(
    private val aiBotSupportRepository: AIBotSupportRepository
) : ViewModel() {
    private val _conversations = MutableStateFlow<List<BotConversation>>(emptyList())
    val conversations: StateFlow<List<BotConversation>> = _conversations.asStateFlow()

    private val _selectedConversation = MutableStateFlow<BotConversation?>(null)
    val selectedConversation: StateFlow<BotConversation?> = _selectedConversation.asStateFlow()

    private val _isLoadingConversation = MutableStateFlow(false)
    val isLoadingConversation: StateFlow<Boolean> = _isLoadingConversation.asStateFlow()

    private val _isLoadingConversations = MutableStateFlow(false)
    val isLoadingConversations: StateFlow<Boolean> = _isLoadingConversations.asStateFlow()

    private val _isBotTyping = MutableStateFlow(false)
    val isBotTyping: StateFlow<Boolean> = _isBotTyping.asStateFlow()

    fun init(accessToken: String, userId: Long) {
        viewModelScope.launch {
            _isLoadingConversations.value = true
            aiBotSupportRepository.init(accessToken, userId)
            _conversations.value = aiBotSupportRepository.loadConversations()
            _isLoadingConversations.value = false
        }
    }

    fun onConversationSelected(conversation: BotConversation) {
        viewModelScope.launch {
            _isLoadingConversation.value = true
            _selectedConversation.value = conversation
            val updatedConversation = aiBotSupportRepository.loadConversation(conversation.id)
            if (updatedConversation != null) {
                _selectedConversation.value = updatedConversation
            }
            _isLoadingConversation.value = false
        }
    }

    fun onNewConversationClicked() {
        val now = Date()
        val greetingMessage = BotMessage(
            id = 0,
            text = "Hi! I'm here to help you with any questions about WordPress. How can I assist you today?",
            date = now,
            isWrittenByUser = false
        )

        _selectedConversation.value = BotConversation(
            id = 0,
            createdAt = now,
            mostRecentMessageDate = now,
            lastMessage = "",
            messages = listOf(greetingMessage)
        )
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
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

            // Show bot typing indicator
            _isBotTyping.value = true

            val conversationId = _selectedConversation.value?.id ?: 0L
            val conversation = if (conversationId == 0L) {
                // This is a new conversation, so we need to create it first
                val newConversation = aiBotSupportRepository.createNewConversation(message)
                if (newConversation != null) {
                    // Add to the top of the conversations list
                    _conversations.value = listOf(newConversation) + _conversations.value
                }
                newConversation
            } else {
                aiBotSupportRepository.sendMessageToConversation(conversationId, message)
            }

            // Hide bot typing indicator
            _isBotTyping.value = false

            if (conversation != null) {
                val finalConversation = conversation.copy(
                    lastMessage = conversation.messages.last().text,
                    messages = (_selectedConversation.value?.messages ?: emptyList()) + conversation.messages
                )
                // Update the conversations list
                _conversations.value = _conversations.value.map {
                    if (it.id == conversationId) {
                        finalConversation
                    } else {
                        it
                    }
                }
                // Update the selected conversation
                _selectedConversation.value = finalConversation
            } else {
                // TODO: show error
            }
        }
    }
}
