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
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.util.generateSampleHESupportConversations
import javax.inject.Inject

@HiltViewModel
class HESupportViewModel @Inject constructor() : ViewModel() {
    sealed class NavigationEvent {
        data class NavigateToConversationDetail(val conversation: SupportConversation) : NavigationEvent()
        data object NavigateToNewTicket : NavigationEvent()
        data object NavigateBack : NavigationEvent()
    }

    private val _conversations = MutableStateFlow<List<SupportConversation>>(emptyList())
    val conversations: StateFlow<List<SupportConversation>> = _conversations.asStateFlow()

    private val _selectedConversation = MutableStateFlow<SupportConversation?>(null)
    val selectedConversation: StateFlow<SupportConversation?> = _selectedConversation.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    fun init() {
        loadDummyData()
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

    fun createNewConversation() {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToNewTicket)
        }
    }

    private fun loadDummyData() {
        _conversations.value = generateSampleHESupportConversations()
    }
}
