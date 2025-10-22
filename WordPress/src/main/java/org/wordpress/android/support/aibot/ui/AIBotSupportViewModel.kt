package org.wordpress.android.support.aibot.ui

import androidx.lifecycle.ViewModel
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
import org.wordpress.android.support.he.ui.HESupportViewModel
import org.wordpress.android.support.model.UserInfo
import org.wordpress.android.util.AppLog
import java.util.Date
import javax.inject.Inject
import kotlin.Long

@HiltViewModel
class AIBotSupportViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val aiBotSupportRepository: AIBotSupportRepository,
    private val appLogWrapper: AppLogWrapper,
) : ViewModel() {
    private val _conversations = MutableStateFlow<List<BotConversation>>(emptyList())
    val conversations: StateFlow<List<BotConversation>> = _conversations.asStateFlow()

    private val _selectedConversation = MutableStateFlow<BotConversation?>(null)
    val selectedConversation: StateFlow<BotConversation?> = _selectedConversation.asStateFlow()

    private val _canSendMessage = MutableStateFlow(true)
    val canSendMessage: StateFlow<Boolean> = _canSendMessage.asStateFlow()

    private val _isLoadingConversation = MutableStateFlow(false)
    val isLoadingConversation: StateFlow<Boolean> = _isLoadingConversation.asStateFlow()

    private val _isLoadingConversations = MutableStateFlow(false)
    val isLoadingConversations: StateFlow<Boolean> = _isLoadingConversations.asStateFlow()

    private val _isBotTyping = MutableStateFlow(false)
    val isBotTyping: StateFlow<Boolean> = _isBotTyping.asStateFlow()

    private val _errorMessage = MutableStateFlow<ErrorType?>(null)
    val errorMessage: StateFlow<ErrorType?> = _errorMessage.asStateFlow()

    private val _userInfo = MutableStateFlow<UserInfo>(UserInfo("", "", "", null))
    val userInfo: StateFlow<UserInfo> = _userInfo.asStateFlow()

    @Suppress("TooGenericExceptionCaught")
    fun init() {
        viewModelScope.launch {
            try {
                // We need to check it this way because access token can be null or empty if not set
                // So, we manually handle it here
                val accessToken = if (accountStore.hasAccessToken()) {
                    accountStore.accessToken!!
                } else {
                    null
                }
                if (accessToken == null) {
                    _errorMessage.value = ErrorType.FORBIDDEN
                    appLogWrapper.e(
                        AppLog.T.SUPPORT, "Error opening the AI bot conversations. The user has no valid access token"
                    )
                } else {
                    aiBotSupportRepository.init(accessToken, accountStore.account.id.toLong())
                    loadUserInfo(accessToken)
                    loadConversations()
                }
            } catch (throwable: Throwable) {
                _errorMessage.value = ErrorType.GENERAL
                appLogWrapper.e(AppLog.T.SUPPORT, "Error initialising the AI bot support repository: " +
                        "${throwable.message} - ${throwable.stackTraceToString()}")
            }
        }
    }

    private fun loadUserInfo(accessToken: String) {
        val account = accountStore.account
        _userInfo.value = UserInfo(
            accessToken = accessToken,
            userName = account.displayName.ifEmpty { account.userName },
            userEmail = account.email,
            avatarUrl = account.avatarUrl.takeIf { it.isNotEmpty() }
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadConversations() {
        try {
            _isLoadingConversations.value = true
            val conversations = aiBotSupportRepository.loadConversations()
            _conversations.value = conversations
        } catch (throwable: Throwable) {
            _errorMessage.value = ErrorType.GENERAL
            appLogWrapper.e(AppLog.T.SUPPORT, "Error loading conversations: " +
                    "${throwable.message} - ${throwable.stackTraceToString()}")
        }
        _isLoadingConversations.value = false
    }

    fun refreshConversations() {
        viewModelScope.launch {
            loadConversations()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    @Suppress("TooGenericExceptionCaught")
    fun onConversationSelected(conversation: BotConversation) {
        viewModelScope.launch {
            try {
                _isLoadingConversation.value = true
                _selectedConversation.value = conversation
                _canSendMessage.value = true
                val updatedConversation = aiBotSupportRepository.loadConversation(conversation.id)
                if (updatedConversation != null) {
                    _selectedConversation.value = updatedConversation
                } else {
                    _errorMessage.value = ErrorType.GENERAL
                    appLogWrapper.e(AppLog.T.SUPPORT, "Error loading conversation: " +
                            "error retrieving it from server")
                }
            } catch (throwable: Throwable) {
                _errorMessage.value = ErrorType.GENERAL
                appLogWrapper.e(AppLog.T.SUPPORT, "Error loading conversation: " +
                        "${throwable.message} - ${throwable.stackTraceToString()}")
            }
            _isLoadingConversation.value = false
        }
    }

    fun onNewConversationClicked() {
        val now = Date()
        _selectedConversation.value = BotConversation(
            id = 0,
            createdAt = now,
            mostRecentMessageDate = now,
            lastMessage = "",
            messages = listOf()
        )
        _canSendMessage.value = true
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

    enum class ErrorType { GENERAL, FORBIDDEN }
}
