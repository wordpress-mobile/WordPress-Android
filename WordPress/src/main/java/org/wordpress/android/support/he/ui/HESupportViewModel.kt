package org.wordpress.android.support.he.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.util.generateSampleSupportConversations
import javax.inject.Inject

@HiltViewModel
class HESupportViewModel @Inject constructor() : ViewModel() {
    private val _conversations = MutableStateFlow<List<SupportConversation>>(emptyList())
    val conversations: StateFlow<List<SupportConversation>> = _conversations.asStateFlow()

    private val _selectedConversation = MutableStateFlow<SupportConversation?>(null)
    val selectedConversation: StateFlow<SupportConversation?> = _selectedConversation.asStateFlow()

    fun init() {
        loadDummyData()
    }

    fun selectConversation(conversation: SupportConversation) {
        _selectedConversation.value = conversation
    }

    fun createNewConversation() {
        // Placeholder for creating new conversation - will be implemented when detail screen is ready
    }

    private fun loadDummyData() {
        _conversations.value = generateSampleSupportConversations()
    }
}
