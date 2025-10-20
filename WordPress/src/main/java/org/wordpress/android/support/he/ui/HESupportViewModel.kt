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
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.util.generateSampleHESupportConversations
import org.wordpress.android.support.model.UserInfo
import javax.inject.Inject

@HiltViewModel
class HESupportViewModel @Inject constructor(
    private val accountStore: AccountStore
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

    fun init() {
        loadDummyData()
        loadUserInfo()
    }

    private fun loadUserInfo() {
        val account = accountStore.account
        _userInfo.value = UserInfo(
            userName = account.displayName.ifEmpty { account.userName },
            userEmail = account.email,
            avatarUrl = account.avatarUrl.takeIf { it.isNotEmpty() }
        )
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

    private fun loadDummyData() {
        _conversations.value = generateSampleHESupportConversations()
    }
}
