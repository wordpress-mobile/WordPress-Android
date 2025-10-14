package org.wordpress.android.support.feature.aibot.ui

import android.util.Log
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

    fun init(accessToken: String, userId: Long) {
        viewModelScope.launch {
            aiBotSupportRepository.init(accessToken, userId)
            val conversations = aiBotSupportRepository.loadConversations()
            Log.d("AI_TAG", "Conversations: ${conversations.size}")
        }
        loadDummyData()
    }

    fun selectConversation(conversation: BotConversation) {
        _selectedConversation.value = conversation
    }

    fun onNewConversationClicked() {
        val now = Date()
        val greetingMessage = BotMessage(
            id = 0,
            text = "Hi! I'm here to help you with any questions about WordPress. How can I assist you today?",
            date = now,
            userWantsToTalkToHuman = false,
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
            if (_selectedConversation.value?.id == 0L) {
                // This is a new conversation, so we need to create it first
                val newConversation = aiBotSupportRepository.createNewConversation(message)

                if (newConversation != null) {
                    // Add to the top of the conversations list
                    _conversations.value = listOf(newConversation) + _conversations.value

                    // Select the new conversation
                    _selectedConversation.value = newConversation
                } else {
                    // TODO: show error
                }
            } else {
                // TODO: just send the message
            }

            // TODO: hide loading
        }
    }

    private fun loadDummyData() {
        _conversations.value = generateSampleBotConversations()
    }
}
