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
    protected val networkUtilsWrapper: NetworkUtilsWrapper,
) : ViewModel() {
    sealed class NavigationEvent {
        data object NavigateToConversationDetail : NavigationEvent()
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
            if (conversations != null) {
                _conversations.value = conversations
                _conversationsState.value = ConversationsState.Loaded
            } else {
                _errorMessage.value = ErrorType.GENERAL
                _conversationsState.value = ConversationsState.Error
                appLogWrapper.e(AppLog.T.SUPPORT, "Error loading support conversations: " +
                        "error retrieving them from server")
            }
        } catch (throwable: Throwable) {
            _errorMessage.value = ErrorType.GENERAL
            _conversationsState.value = ConversationsState.Error
            appLogWrapper.e(
                AppLog.T.SUPPORT, "Error loading support conversations: " +
                        "${throwable.message} - ${throwable.stackTraceToString()}"
            )
        }
    }

    /** Returns the conversations, or null when they could not be retrieved. */
    protected abstract suspend fun getConversations(): List<ConversationType>?

    fun refreshConversations() {
        viewModelScope.launch {
            loadConversations(isRefresh = true)
        }
    }

    // Guards against overlapping silent refreshes (e.g. the minute timer and onStart firing close
    // together) whose out-of-order responses could show an older list. Confined to the main thread
    // (viewModelScope runs on Main), so no synchronization is needed.
    private var isSilentRefreshInFlight = false

    /**
     * Reloads the conversation list from the server without showing the pull-to-refresh spinner or a
     * blocking loader. Used by the list screen's periodic auto-refresh and its refresh-on-resume so
     * new or updated conversations appear while the screen stays open. Leaves the current list and
     * error state untouched on failure (a background refresh should not surface an error).
     */
    @Suppress("TooGenericExceptionCaught")
    fun refreshConversationsSilently() {
        // Skip while a visible load or pull-to-refresh owns the state (so we neither cut its spinner
        // short nor overwrite its newer result), and coalesce overlapping silent refreshes.
        val state = _conversationsState.value
        if (isSilentRefreshInFlight ||
            state == ConversationsState.Loading ||
            state == ConversationsState.Refreshing
        ) {
            return
        }
        isSilentRefreshInFlight = true
        viewModelScope.launch {
            try {
                if (!networkUtilsWrapper.isNetworkAvailable()) return@launch
                val conversations = getConversations()
                // A visible load/refresh may have started while we were fetching; don't stomp it.
                val currentState = _conversationsState.value
                if (conversations != null &&
                    currentState != ConversationsState.Loading &&
                    currentState != ConversationsState.Refreshing
                ) {
                    _conversations.value = conversations
                    _conversationsState.value = ConversationsState.Loaded
                }
            } catch (throwable: Throwable) {
                appLogWrapper.e(
                    AppLog.T.SUPPORT, "Error silently refreshing support conversations: " +
                            "${throwable.message} - ${throwable.stackTraceToString()}"
                )
            } finally {
                isSilentRefreshInFlight = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    suspend fun setNewConversation(conversation: ConversationType) {
        onConversationOpened()
        _selectedConversation.value = conversation
        _navigationEvents.emit(NavigationEvent.NavigateToConversationDetail)
    }

    /**
     * Hook invoked whenever a conversation is opened (either an existing one or a new one). Subclasses
     * can override this to reset any per-conversation transient state (e.g. a draft reply form), so it
     * does not leak into the next conversation regardless of how the user navigated away from the
     * previous one (toolbar back, system back button, or back gesture).
     */
    protected open fun onConversationOpened() {}

    // Region navigation

    @Suppress("TooGenericExceptionCaught")
    fun onConversationClick(conversation: ConversationType) {
        viewModelScope.launch {
            try {
                if (!networkUtilsWrapper.isNetworkAvailable()) {
                    _errorMessage.value = ErrorType.OFFLINE
                    return@launch
                }

                onConversationOpened()
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

    // End region

    enum class ErrorType {
        GENERAL,
        FORBIDDEN,
        OFFLINE,
    }
}
