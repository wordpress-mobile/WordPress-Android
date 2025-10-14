package org.wordpress.android.support.feature.aibot.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.support.feature.aibot.util.generateSampleBotConversations
import org.wordpress.android.support.feature.aibot.model.BotConversation
import org.wordpress.android.support.feature.aibot.model.BotMessage
import org.wordpress.android.support.feature.aibot.repository.AIBotSupportRepository
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.BotConversationSummary
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpAuthenticationProvider
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AIBotSupportViewModel @Inject constructor(
    private val aiBotSupportRepository: AIBotSupportRepository
) : ViewModel() {
    private val _conversations = MutableStateFlow<List<BotConversation>>(emptyList())
    val conversations: StateFlow<List<BotConversation>> = _conversations.asStateFlow()

    private val _selectedConversation = MutableStateFlow<BotConversation?>(null)
    val selectedConversation: StateFlow<BotConversation?> = _selectedConversation.asStateFlow()

    fun init(accessToken: String, userId: Long) {
        viewModelScope.launch {
            aiBotSupportRepository.init(accessToken, userId)
//            aiBotSupportRepository.loadConversations()
        }
        loadDummyData()
    }

    fun selectConversation(conversation: BotConversation) {
        _selectedConversation.value = conversation
    }

    fun createNewConversation() {
        val now = Date()

        // Create initial bot greeting message
        val greetingMessage = BotMessage(
            id = 0,
            text = "Hi! I'm here to help you with any questions about WordPress. How can I assist you today?",
            date = now,
            userWantsToTalkToHuman = false,
            isWrittenByUser = false
        )

        val newConversation = BotConversation(
            id = 0,
            mostRecentMessageDate = now,
            messages = listOf(greetingMessage),
            createdAt = now,
            lastMessage = ""
        )

        // Add to the top of the conversations list
        _conversations.value = listOf(newConversation) + _conversations.value

        // Select the new conversation
        _selectedConversation.value = newConversation
    }

    fun sendMessage(text: String) {
        val currentConversation = _selectedConversation.value ?: return
        val now = Date()

        // Create new user message
        val userMessage = BotMessage(
            id = System.currentTimeMillis(),
            text = text,
            date = now,
            userWantsToTalkToHuman = false,
            isWrittenByUser = true
        )

        // Create bot response (dummy response for now)
        val botMessage = BotMessage(
            id = System.currentTimeMillis() + 1,
            text = "Thanks for your message! This is a dummy response. In a real implementation, this would connect to the support bot API.",
            date = Date(now.time + 1000),
            userWantsToTalkToHuman = false,
            isWrittenByUser = false
        )

        // Update conversation with new messages
        val updatedMessages = currentConversation.messages + listOf(userMessage, botMessage)
        val updatedConversation = currentConversation.copy(
            messages = updatedMessages,
            mostRecentMessageDate = botMessage.date
        )

        // Update the conversation in the list
        _conversations.value = _conversations.value.map { conversation ->
            if (conversation.id == updatedConversation.id) {
                updatedConversation
            } else {
                conversation
            }
        }

        // Update selected conversation
        _selectedConversation.value = updatedConversation
    }

    private fun loadDummyData() {
        _conversations.value = generateSampleBotConversations()
    }
}
