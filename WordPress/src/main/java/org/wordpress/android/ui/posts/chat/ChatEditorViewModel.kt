package org.wordpress.android.ui.posts.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

private const val HEADER_LENGTH_THRESHOLD = 4

class ChatEditorViewModel @Inject constructor() : ViewModel() {
    private val _onNewContent = SingleLiveEvent<String>()
    val onNewContent: LiveData<String> = _onNewContent

    private val _onClearInput = SingleLiveEvent<Unit>()
    val onClearInput: LiveData<Unit> = _onClearInput

    private val _onAttachRequest = SingleLiveEvent<Unit>()
    val onAttachRequest: LiveData<Unit> = _onAttachRequest

    fun onSendButtonPressed(text: String) {
        _onClearInput.call()
        _onNewContent.value = getContent(text)
    }

    fun onAttachButtonPressed() {
        _onAttachRequest.call()
    }

    private fun getContent(text: String): String {
        return if (text.wordCount < HEADER_LENGTH_THRESHOLD) {
            text.toHeading
        } else {
            text.toParagraph
        }
    }
}
