package org.wordpress.android.ui.reader.subscription

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.subscription.ReaderBlogSubscriptionUseCase.UpdateResult
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ReaderSubscriptionSettingsViewModel @Inject constructor(
    private val subscriptionUseCase: ReaderBlogSubscriptionUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _uiState = MutableStateFlow<ReaderSubscriptionSettingsUiState?>(null)
    val uiState: StateFlow<ReaderSubscriptionSettingsUiState?> = _uiState

    private val _snackbarEvents = MutableLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    fun start(blogId: Long, blogName: String, blogUrl: String) {
        viewModelScope.launch(bgDispatcher) {
            val subscription = subscriptionUseCase.getSubscriptionForBlog(blogId)

            _uiState.value = ReaderSubscriptionSettingsUiState(
                blogId = blogId,
                blogName = blogName,
                blogUrl = blogUrl,
                notifyPostsEnabled = subscription?.shouldNotifyPosts ?: false,
                emailPostsEnabled = subscription?.shouldEmailPosts ?: false,
                emailCommentsEnabled = subscription?.shouldEmailComments ?: false
            )
        }
    }

    fun onNotifyPostsToggled(enabled: Boolean) {
        val currentState = _uiState.value ?: return
        if (currentState.isLoading) return
        val previousValue = currentState.notifyPostsEnabled
        _uiState.value = currentState.copy(notifyPostsEnabled = enabled, isLoading = true)

        viewModelScope.launch {
            val result = subscriptionUseCase.updateNotifyPosts(currentState.blogId, enabled)
            handleUpdateResult(result, enabled, previousValue) { state, value ->
                state.copy(notifyPostsEnabled = value)
            }
        }
    }

    fun onEmailPostsToggled(enabled: Boolean) {
        val currentState = _uiState.value ?: return
        if (currentState.isLoading) return
        val previousValue = currentState.emailPostsEnabled
        _uiState.value = currentState.copy(emailPostsEnabled = enabled, isLoading = true)

        viewModelScope.launch {
            val result = subscriptionUseCase.updateEmailPosts(currentState.blogId, enabled)
            handleUpdateResult(result, enabled, previousValue) { state, value ->
                state.copy(emailPostsEnabled = value)
            }
        }
    }

    fun onEmailCommentsToggled(enabled: Boolean) {
        val currentState = _uiState.value ?: return
        if (currentState.isLoading) return
        val previousValue = currentState.emailCommentsEnabled
        _uiState.value = currentState.copy(emailCommentsEnabled = enabled, isLoading = true)

        viewModelScope.launch {
            val result = subscriptionUseCase.updateEmailComments(currentState.blogId, enabled)
            handleUpdateResult(result, enabled, previousValue) { state, value ->
                state.copy(emailCommentsEnabled = value)
            }
        }
    }

    private fun handleUpdateResult(
        result: UpdateResult,
        newValue: Boolean,
        previousValue: Boolean,
        updateState: (ReaderSubscriptionSettingsUiState, Boolean) -> ReaderSubscriptionSettingsUiState
    ) {
        val currentState = _uiState.value ?: return

        when (result) {
            is UpdateResult.Success -> {
                _uiState.value = updateState(currentState, newValue).copy(isLoading = false)
            }
            is UpdateResult.NoNetwork -> {
                // Revert to previous value on no network
                _uiState.value = updateState(currentState, previousValue).copy(isLoading = false)
                _snackbarEvents.postValue(
                    Event(SnackbarMessageHolder(UiStringRes(R.string.no_network_message)))
                )
            }
            is UpdateResult.Failure -> {
                // Revert to previous value on failure
                _uiState.value = updateState(currentState, previousValue).copy(isLoading = false)
                _snackbarEvents.postValue(
                    Event(SnackbarMessageHolder(UiStringRes(R.string.reader_subscription_settings_update_error)))
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        subscriptionUseCase.cleanup()
    }
}
