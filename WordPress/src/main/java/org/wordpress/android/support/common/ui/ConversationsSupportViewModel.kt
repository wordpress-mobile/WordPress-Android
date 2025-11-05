package org.wordpress.android.support.common.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.common.model.Conversation
import org.wordpress.android.support.common.model.UserInfo
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper

abstract class ConversationsSupportViewModel<ConversationType: Conversation>(
    protected val accountStore: AccountStore,
    protected val appLogWrapper: AppLogWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
) : ViewModel() {
    sealed class NavigationEvent {
        data object NavigateToConversationDetail : NavigationEvent()
        data object NavigateToNewConversation : NavigationEvent()
        data object NavigateBack : NavigationEvent()
    }

    sealed class ConversationsState {
        data object Loading : ConversationsState()
        data object Refreshing : ConversationsState()
        data object Loaded : ConversationsState()
        data object NoNetwork : ConversationsState()
        data object Error : ConversationsState()
    }

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    @Suppress("VariableNaming")
    protected val _conversations = MutableStateFlow<List<ConversationType>>(emptyList())
    val conversations: StateFlow<List<ConversationType>> = _conversations.asStateFlow()

    private val _isLoadingConversation = MutableStateFlow(false)
    val isLoadingConversation: StateFlow<Boolean> = _isLoadingConversation.asStateFlow()

    @Suppress("VariableNaming")
    protected val _selectedConversation = MutableStateFlow<ConversationType?>(null)
    val selectedConversation: StateFlow<ConversationType?> = _selectedConversation.asStateFlow()

    @Suppress("VariableNaming")
    protected val _userInfo = MutableStateFlow(UserInfo("", "", ""))
    val userInfo: StateFlow<UserInfo> = _userInfo.asStateFlow()

    @Suppress("VariableNaming")
    protected val _conversationsState = MutableStateFlow<ConversationsState>(ConversationsState.Loading)
    val conversationsState: StateFlow<ConversationsState> = _conversationsState.asStateFlow()

    @Suppress("VariableNaming")
    protected val _errorMessage = MutableStateFlow<ErrorType?>(null)
    val errorMessage: StateFlow<ErrorType?> = _errorMessage.asStateFlow()

    @Suppress("TooGenericExceptionCaught")
    fun init() {
        viewModelScope.launch {
            try {
                val accessToken = accountStore.accessToken.takeIf { accountStore.hasAccessToken() }
                if (accessToken == null) {
                    _errorMessage.value = ErrorType.FORBIDDEN
                    appLogWrapper.e(
                        AppLog.T.SUPPORT, "Error initialising support conversations: The user has no valid access token"
                    )
                } else {
                    initRepository(accessToken)
                    loadUserInfo()
                    loadConversations()
                }
            } catch (throwable: Throwable) {
                _errorMessage.value = ErrorType.GENERAL
                appLogWrapper.e(AppLog.T.SUPPORT, "Error initialising support conversations: " +
                        "${throwable.message} - ${throwable.stackTraceToString()}")
            }
        }
    }

    abstract fun initRepository(accessToken: String)

    protected fun loadUserInfo() {
        val account = accountStore.account
        _userInfo.value = UserInfo(
            userName = account.displayName.ifEmpty { account.userName },
            userEmail = account.email,
            avatarUrl = account.avatarUrl.takeIf { it.isNotEmpty() }
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadConversations(isRefresh: Boolean = false) {
        try {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                _conversationsState.value = ConversationsState.NoNetwork
                return
            }

            _conversationsState.value = if (isRefresh) ConversationsState.Refreshing else ConversationsState.Loading
            val conversations = getConversations()
            _conversations.value = conversations
            _conversationsState.value = ConversationsState.Loaded
        } catch (throwable: Throwable) {
            _errorMessage.value = ErrorType.GENERAL
            _conversationsState.value = ConversationsState.Error
            appLogWrapper.e(
                AppLog.T.SUPPORT, "Error loading support conversations: " +
                        "${throwable.message} - ${throwable.stackTraceToString()}"
            )
        }
    }

    protected abstract suspend fun getConversations(): List<ConversationType>

    fun refreshConversations() {
        viewModelScope.launch {
            loadConversations(isRefresh = true)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    suspend fun setNewConversation(conversation: ConversationType) {
        _selectedConversation.value = conversation
        _navigationEvents.emit(NavigationEvent.NavigateToConversationDetail)
    }

    // Region navigation

    @Suppress("TooGenericExceptionCaught")
    fun onConversationClick(conversation: ConversationType) {
        viewModelScope.launch {
            try {
                _isLoadingConversation.value = true
                _selectedConversation.value = conversation
                _navigationEvents.emit(NavigationEvent.NavigateToConversationDetail)

                val updatedConversation = getConversation(conversation.getConversationId())
                if (updatedConversation != null) {
                    // refresh selected conversation
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

    abstract suspend fun getConversation(conversationId: Long): ConversationType?

    fun onBackClick() {
        viewModelScope.launch {
            _selectedConversation.value = null
            _navigationEvents.emit(NavigationEvent.NavigateBack)
        }
    }

    fun onCreateNewConversationClick() {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToNewConversation)
        }
    }

    // End region

    enum class ErrorType {
        GENERAL,
        FORBIDDEN,
    }
}
