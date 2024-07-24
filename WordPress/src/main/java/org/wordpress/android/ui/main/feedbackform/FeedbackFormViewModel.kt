package org.wordpress.android.ui.main.feedbackform

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.support.ZendeskHelper
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject

@HiltViewModel
class FeedbackFormViewModel @Inject constructor() : ViewModel() {
    @Inject
    lateinit var zendeskHelper: ZendeskHelper

    private val _messageText = MutableStateFlow("")
    val messageText = _messageText.asStateFlow()

    private val _isProgressShowing = MutableStateFlow<Boolean?>(null)
    val isProgressShowing = _isProgressShowing.asStateFlow()

    fun updateMessageText(message: String) {
        if (message != _messageText.value) {
            _messageText.value = message
        }
    }

    fun onSubmitClick(context: Context) {
        if (NetworkUtils.checkConnection(context)) {
            viewModelScope.launch(Dispatchers.Default) {
                _isProgressShowing.value = true
                try {
                    submitRequest(context)
                } finally {
                    _isProgressShowing.value = false
                }
            }
        }
    }

    private suspend fun submitRequest(context: Context) {
        zendeskHelper.requireIdentity(context, selectedSiteFromExtras) {
            showTicketCreatingMessage()

            val description = zendeskHelper.parseChatHistory(
                getString(R.string.contact_support_bot_ticket_comment_start),
                getString(R.string.contact_support_bot_ticket_comment_question),
                getString(R.string.contact_support_bot_ticket_comment_answer),
                chatHistory
            )
            createNewZendeskRequest(description, object : ZendeskHelper.CreateRequestCallback() {
                override fun onSuccess() {
                    onSuccess(context)
                }
                override fun onError(errorMessage: String?) {
                    onFailure(context, errorMessage)
                }
            })
        }
    }

    fun onCloseClick(context: Context) {
        (context as? Activity)?.let { activity ->
            if (_messageText.value.isEmpty()) {
                activity.finish()
            } else {
                confirmDiscard(activity)
            }
        }
    }

    private fun confirmDiscard(activity: Activity) {
        MaterialAlertDialogBuilder(activity).also { builder ->
            builder.setTitle(R.string.feedback_form_discard)
            builder.setPositiveButton(R.string.discard) { _, _ ->
                activity.finish()
            }
            builder.setNegativeButton(R.string.cancel) { _, _ ->
            }
            builder.show()
        }
    }

    private fun onSuccess(context: Context) {
        Toast.makeText(context, R.string.feedback_form_success, Toast.LENGTH_LONG).show()
        (context as? Activity)?.finish()
    }

    private fun onFailure(context: Context, errorMessage: String? = null) {
        Toast.makeText(context, R.string.feedback_form_failure, Toast.LENGTH_LONG).show()
    }
}

