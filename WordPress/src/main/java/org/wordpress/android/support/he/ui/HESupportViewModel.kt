package org.wordpress.android.support.he.ui

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
import org.wordpress.android.support.common.ui.SupportViewModel
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.repository.CreateConversationResult
import org.wordpress.android.support.he.repository.HESupportRepository
import org.wordpress.android.util.AppLog
import javax.inject.Inject

@HiltViewModel
class HESupportViewModel @Inject constructor(
    accountStore: AccountStore,
    private val heSupportRepository: HESupportRepository,
    appLogWrapper: AppLogWrapper,
) : SupportViewModel<SupportConversation>(accountStore, appLogWrapper) {
    sealed class NavigationEvent {
        data class NavigateToConversationDetail(val conversation: SupportConversation) : NavigationEvent()
        data object NavigateToNewTicket : NavigationEvent()
        data object NavigateBack : NavigationEvent()
    }

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    private val _isSendingNewConversation = MutableStateFlow(false)
    val isSendingNewConversation: StateFlow<Boolean> = _isSendingNewConversation.asStateFlow()

    override fun initRepository(accessToken: String) {
        heSupportRepository.init(accessToken)
    }

    override suspend fun getConversations(): List<SupportConversation> = heSupportRepository.loadConversations()

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
}
