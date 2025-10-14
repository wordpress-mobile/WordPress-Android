package org.wordpress.android.support.feature.aibot.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.support.feature.aibot.model.BotConversation
import org.wordpress.android.support.feature.aibot.model.BotMessage
import org.wordpress.android.support.feature.aibot.repository.AIBotSupportRepository
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

    fun init(accessToken: String, userId: Long) {
        viewModelScope.launch {
            aiBotSupportRepository.init(accessToken, userId)
            _conversations.value = aiBotSupportRepository.loadConversations()
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
        // TODO: show loading

        viewModelScope.launch {
            val conversationId = _selectedConversation.value?.id ?: -1
            val conversation = if (conversationId == -1L) {
                // This is a new conversation, so we need to create it first
                aiBotSupportRepository.createNewConversation(message)
            } else {
                aiBotSupportRepository.sendMessageToConversation(conversationId, message)
            }

            if (conversation != null) {
                // Add to the top of the conversations list
                _conversations.value = listOf(conversation) + _conversations.value

                // Select the new conversation
                _selectedConversation.value = conversation
            } else {
                // TODO: show error
            }

            // TODO: hide loading
        }
    }
}
