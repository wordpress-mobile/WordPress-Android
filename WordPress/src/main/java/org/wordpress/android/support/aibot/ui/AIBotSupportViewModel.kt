package org.wordpress.android.support.aibot.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.aibot.model.BotConversation
import org.wordpress.android.support.aibot.model.BotMessage
import org.wordpress.android.support.aibot.repository.AIBotSupportRepository
import org.wordpress.android.support.common.ui.ConversationsSupportViewModel
import org.wordpress.android.ui.compose.utils.markdownToAnnotatedString
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AIBotSupportViewModel @Inject constructor(
    accountStore: AccountStore,
    private val aiBotSupportRepository: AIBotSupportRepository,
    appLogWrapper: AppLogWrapper,
    networkUtilsWrapper: NetworkUtilsWrapper,
) : ConversationsSupportViewModel<BotConversation>(accountStore, appLogWrapper, networkUtilsWrapper) {
    private val _canSendMessage = MutableStateFlow(true)
    val canSendMessage: StateFlow<Boolean> = _canSendMessage.asStateFlow()

    private val _isBotTyping = MutableStateFlow(false)
    val isBotTyping: StateFlow<Boolean> = _isBotTyping.asStateFlow()

    private val _isLoadingOlderMessages = MutableStateFlow(false)
    val isLoadingOlderMessages: StateFlow<Boolean> = _isLoadingOlderMessages.asStateFlow()

    private val _hasMorePages = MutableStateFlow(true)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages.asStateFlow()

    private val paginationMutex = Mutex()
    private var currentPage = 1L

    override fun initRepository(accessToken: String) {
        aiBotSupportRepository.init(accessToken, accountStore.account.userId)
    }

    override suspend fun getConversations() = aiBotSupportRepository.loadConversations()

    override suspend fun getConversation(conversationId: Long): BotConversation? {
        _canSendMessage.value = false
        currentPage = 1L
        _hasMorePages.value = true
        return aiBotSupportRepository.loadConversation(conversationId, pageNumber = currentPage).also { conversation ->
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
            currentPage = 1L
            _hasMorePages.value = false
            setNewConversation(botConversation)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun loadOlderMessages() {
        if (!_hasMorePages.value || _isLoadingOlderMessages.value) {
            return
        }

        viewModelScope.launch {
            // Use mutex to prevent concurrent pagination requests
            paginationMutex.withLock {
                // Double-check conditions after acquiring lock
                if (!_hasMorePages.value || _isLoadingOlderMessages.value) {
                    return@launch
                }

                try {
                    _isLoadingOlderMessages.value = true
                    val conversationId = _selectedConversation.value?.id ?: return@withLock

                    currentPage++
                    val olderMessagesConversation = aiBotSupportRepository.loadConversation(
                        conversationId,
                        pageNumber = currentPage
                    )

                    if (olderMessagesConversation != null) {
                        val olderMessages = olderMessagesConversation.messages

                        // Check if we've reached the end (empty messages)
                        if (olderMessages.isEmpty()) {
                            _hasMorePages.value = false
                        } else {
                            // Prepend older messages to the existing ones
                            // (older messages go at the beginning of the list)
                            val currentMessages = _selectedConversation.value?.messages ?: emptyList()
                            _selectedConversation.value = _selectedConversation.value?.copy(
                                messages = olderMessages + currentMessages
                            )
                        }
                    } else {
                        // Error loading, stay on current page
                        currentPage--
                        _errorMessage.value = ErrorType.GENERAL
                        appLogWrapper.e(AppLog.T.SUPPORT, "Error loading older messages: response is null")
                    }
                } catch (throwable: Throwable) {
                    currentPage--
                    _errorMessage.value = ErrorType.GENERAL
                    appLogWrapper.e(AppLog.T.SUPPORT, "Error loading older messages: " +
                            "${throwable.message} - ${throwable.stackTraceToString()}")
                } finally {
                    _isLoadingOlderMessages.value = false
                }
            }
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
                val botMessage = BotMessage(
                    id = System.currentTimeMillis(),
                    rawText = message,
                    formattedText = markdownToAnnotatedString(message),
                    date = now,
                    isWrittenByUser = true
                )
                val currentMessages = (_selectedConversation.value?.messages ?: emptyList()) + botMessage
                _selectedConversation.value = _selectedConversation.value?.copy(
                    messages = currentMessages
                )

                val conversation = sendMessageToBot(message)

                // Hide bot typing indicator
                _isBotTyping.value = false

                if (conversation != null) {
                    val finalConversation = conversation.copy(
                        lastMessage = conversation.messages.last().rawText,
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
