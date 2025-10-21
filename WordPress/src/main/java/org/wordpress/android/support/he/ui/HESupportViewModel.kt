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
import org.wordpress.android.support.he.repository.HESupportRepository
import org.wordpress.android.support.he.util.generateSampleHESupportConversations
import org.wordpress.android.support.model.UserInfo
import org.wordpress.android.util.AppLog
import javax.inject.Inject

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

    private val _conversations = MutableStateFlow<List<SupportConversation>>(emptyList())
    val conversations: StateFlow<List<SupportConversation>> = _conversations.asStateFlow()

    private val _selectedConversation = MutableStateFlow<SupportConversation?>(null)
    val selectedConversation: StateFlow<SupportConversation?> = _selectedConversation.asStateFlow()

    private val _userInfo = MutableStateFlow(UserInfo())
    val userInfo: StateFlow<UserInfo> = _userInfo.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    private val _isLoadingConversations = MutableStateFlow(false)
    val isLoadingConversations: StateFlow<Boolean> = _isLoadingConversations.asStateFlow()

    private val _errorMessage = MutableStateFlow<ErrorType?>(null)
    val errorMessage: StateFlow<ErrorType?> = _errorMessage.asStateFlow()

    fun init() {
        loadUserInfo()
        loadConversations()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            if (!accountStore.hasAccessToken()) {
                _errorMessage.value = ErrorType.FORBIDDEN
                _navigationEvents.emit(NavigationEvent.NavigateBack)
                return@launch
            }
            val accessToken = accountStore.accessToken!!
            val account = accountStore.account
            heSupportRepository.init(accessToken)
            _userInfo.value = UserInfo(
                accessToken = accessToken,
                userName = account.displayName.ifEmpty { account.userName },
                userEmail = account.email,
                avatarUrl = account.avatarUrl.takeIf { it.isNotEmpty() }
            )
        }
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

    fun onSendNewConversation() {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateBack)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    enum class ErrorType { GENERAL, FORBIDDEN }
}
