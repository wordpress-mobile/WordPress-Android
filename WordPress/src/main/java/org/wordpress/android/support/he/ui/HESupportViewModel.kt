package org.wordpress.android.support.he.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.repository.CreateConversationResult
import org.wordpress.android.support.he.repository.HESupportRepository
import org.wordpress.android.support.model.UserInfo
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import kotlin.String

@HiltViewModel
class HESupportViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val heSupportRepository: HESupportRepository,
    private val appLogWrapper: AppLogWrapper,
) : ViewModel() {
    sealed class NavigationEvent {
        data class NavigateToConversationDetail(val conversation: SupportConversation) : NavigationEvent()
        data object NavigateToNewTicket : NavigationEvent()
        data object NavigateBack : NavigationEvent()
    }

    private val _conversations = MutableStateFlow<List<SupportConversation>>(listOf())
    val conversations: StateFlow<List<SupportConversation>> = _conversations.asStateFlow()

    private val _selectedConversation = MutableStateFlow<SupportConversation?>(null)
    val selectedConversation: StateFlow<SupportConversation?> = _selectedConversation.asStateFlow()

    private val _userInfo = MutableStateFlow<UserInfo>(UserInfo("", "", "", null))
    val userInfo: StateFlow<UserInfo> = _userInfo.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    private val _isLoadingConversations = MutableStateFlow(false)
    val isLoadingConversations: StateFlow<Boolean> = _isLoadingConversations.asStateFlow()

    private val _isSendingNewConversation = MutableStateFlow(false)
    val isSendingNewConversation: StateFlow<Boolean> = _isSendingNewConversation.asStateFlow()

    private val _errorMessage = MutableStateFlow<ErrorType?>(null)
    val errorMessage: StateFlow<ErrorType?> = _errorMessage.asStateFlow()

    fun init() {
        viewModelScope.launch {
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
                    AppLog.T.SUPPORT, "Error opening HE conversations. The user has no valid access token"
                )
            } else {
                loadUserInfo(accessToken)
                loadConversations()
            }
        }
    }

    private fun loadUserInfo(accessToken: String) {
        val account = accountStore.account
        heSupportRepository.init(accessToken)
        _userInfo.value = UserInfo(
            accessToken = accessToken,
            userName = account.displayName.ifEmpty { account.userName },
            userEmail = account.email,
            avatarUrl = account.avatarUrl.takeIf { it.isNotEmpty() }
        )
    }

    private fun loadConversations() {
        viewModelScope.launch {
            try {
                _isLoadingConversations.value = true
                val conversations = heSupportRepository.loadConversations()
                _conversations.value = conversations
            } catch (throwable: Throwable) {
                _errorMessage.value = ErrorType.GENERAL
                appLogWrapper.e(
                    AppLog.T.SUPPORT, "Error loading HE conversations: " +
                            "${throwable.message} - ${throwable.stackTraceToString()}"
                )
            }
            _isLoadingConversations.value = false
        }
    }

    fun refreshConversations() {
        loadConversations()
    }

    fun onConversationClick(conversation: SupportConversation) {
        viewModelScope.launch {
            _selectedConversation.value = conversation
            _navigationEvents.emit(NavigationEvent.NavigateToConversationDetail(conversation))
        }
    }

    fun onBackFromDetailClick() {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateBack)
        }
    }

    fun onCreateNewConversation() {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToNewTicket)
        }
    }

    fun onSendNewConversation(
        subject: String,
        message: String,
        tags: List<String>,
        attachments: List<String>
    ) {
        viewModelScope.launch {
            _isSendingNewConversation.value = true

            when (val result = heSupportRepository.createConversation(
                subject = subject,
                message = message,
                tags = tags,
                attachments = attachments
            )) {
                is CreateConversationResult.Success -> {
                    _selectedConversation.value = result.conversation
                    _navigationEvents.emit(NavigationEvent.NavigateToConversationDetail(result.conversation))
                }

                is CreateConversationResult.Error.Unauthorized -> {
                    _errorMessage.value = ErrorType.FORBIDDEN
                    appLogWrapper.e(AppLog.T.SUPPORT, "Unauthorized error creating HE conversation")
                }

                is CreateConversationResult.Error.GeneralError -> {
                    _errorMessage.value = ErrorType.GENERAL
                    appLogWrapper.e(AppLog.T.SUPPORT, "General error creating HE conversation")
                }
            }

            _isSendingNewConversation.value = false
        }
    }

    fun onAddMessageToConversation(
        message: String,
        attachments: List<String>
    ) {
        viewModelScope.launch {
            val selectedConversation = _selectedConversation.value
            if (selectedConversation == null) {
                appLogWrapper.e(AppLog.T.SUPPORT, "Error answering a conversation: no conversation selected")
                return@launch
            }

            _isSendingNewConversation.value = true

            when (val result = heSupportRepository.addMessageToConversation(
                conversationId = selectedConversation.id,
                message = message,
                attachments = attachments
            )) {
                is CreateConversationResult.Success -> {
                    _selectedConversation.value = result.conversation
                    // TODO refresh conversation and scroll to bottom
                }

                is CreateConversationResult.Error.Unauthorized -> {
                    _errorMessage.value = ErrorType.FORBIDDEN
                    appLogWrapper.e(AppLog.T.SUPPORT, "Unauthorized error adding message to HE conversation")
                }

                is CreateConversationResult.Error.GeneralError -> {
                    _errorMessage.value = ErrorType.GENERAL
                    appLogWrapper.e(AppLog.T.SUPPORT, "General error adding message to HE conversation")
                }
            }

            _isSendingNewConversation.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    enum class ErrorType {
        GENERAL,
        FORBIDDEN,
    }
}
