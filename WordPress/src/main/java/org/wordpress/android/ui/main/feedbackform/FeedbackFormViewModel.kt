package org.wordpress.android.ui.main.feedbackform

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject

@HiltViewModel
class FeedbackFormViewModel @Inject constructor( ) : ViewModel() {
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
        if (_messageText.value.isEmpty()) {
            return
        }
        if (!NetworkUtils.checkConnection(context)) {
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            _isProgressShowing.value = true
            delay(1500L) // TODO submit request
            _isProgressShowing.value = false
            withContext(Dispatchers.Main) {
                onSuccess(context)
            }
        }
    }

    fun onCloseClick(activity: Activity?) {
        activity?.let {
            if (_messageText.value.isEmpty()) {
                it.finish()
            } else {
                confirmDiscard(it)
            }
        }
    }

    private fun onSuccess(context: Context) {
        Toast.makeText(context, R.string.feedback_form_success, Toast.LENGTH_SHORT).show()
        (context as? Activity)?.finish()
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
}

