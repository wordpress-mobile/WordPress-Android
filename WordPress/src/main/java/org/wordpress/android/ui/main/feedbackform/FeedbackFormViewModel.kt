package org.wordpress.android.ui.main.feedbackform

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject


@HiltViewModel
class FeedbackFormViewModel @Inject constructor(
) : ViewModel() {
    private val _messageText = MutableStateFlow("")
    val messageText = _messageText.asStateFlow()

    fun updateMessageText(message: String) {
        if (message != _messageText.value) {
            _messageText.value = message
        }
    }

    fun onSubmitClick(context: Context) {
        if (_messageText.value.isEmpty()) {
            return
        }

        if (NetworkUtils.checkConnection(context)) {
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            // TODO show progress and submit request
        }
    }
}

