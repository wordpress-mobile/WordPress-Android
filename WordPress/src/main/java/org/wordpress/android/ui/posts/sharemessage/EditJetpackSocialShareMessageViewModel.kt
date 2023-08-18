package org.wordpress.android.ui.posts.sharemessage

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.posts.sharemessage.EditJetpackSocialShareMessageViewModel.UiState.Initial
import org.wordpress.android.ui.posts.sharemessage.EditJetpackSocialShareMessageViewModel.UiState.Loaded
import org.wordpress.android.util.StringProvider
import javax.inject.Inject

@HiltViewModel
class EditJetpackSocialShareMessageViewModel @Inject constructor(
    private val stringProvider: StringProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(Initial)
    val uiState: StateFlow<UiState> = _uiState

    private val _actionEvents = Channel<ActionEvent>(Channel.BUFFERED)
    val actionEvents = _actionEvents.receiveAsFlow()

    private var currentShareMessage = ""
    private var hasChangedMessage = false

    fun start(initialShareMessage: String) {
        if (!hasChangedMessage) currentShareMessage = initialShareMessage
        _uiState.value = Loaded(
            appBarLabel = stringProvider.getString(
                R.string.post_settings_jetpack_social_share_message_title
            ),
            currentShareMessage = currentShareMessage,
            shareMessageMaxLength = SHARE_MESSAGE_MAX_LENGTH,
            customizeMessageDescription = stringProvider.getString(
                R.string.post_settings_jetpack_social_share_message_description
            ),
            onBackClick = ::onBackClick,
        )
    }

    fun updateShareMessage(shareMessage: String) {
        currentShareMessage = shareMessage
        hasChangedMessage = true
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun onBackClick() {
        viewModelScope.launch {
            _actionEvents.send(
                ActionEvent.FinishActivity(
                    updatedShareMessage = currentShareMessage.trim(),
                )
            )
        }
    }

    sealed class UiState {
        object Initial : UiState()

        data class Loaded(
            val appBarLabel: String,
            val currentShareMessage: String,
            val shareMessageMaxLength: Int,
            val customizeMessageDescription: String,
            val onBackClick: () -> Unit,
        ) : UiState()
    }

    sealed class ActionEvent {
        data class FinishActivity(val updatedShareMessage: String) : ActionEvent()
    }
}

private const val SHARE_MESSAGE_MAX_LENGTH = 255
