package org.wordpress.android.ui.main.feedbackform

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.R
import org.wordpress.android.support.ZendeskHelper
import org.wordpress.android.ui.accounts.HelpActivity
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
        if (!NetworkUtils.checkConnection(context)) {
            return
        }

        _isProgressShowing.value = true

        zendeskHelper.requireIdentity(context, selectedSite = null) {
            createNewZendeskRequest(
                context = context,
                description = _messageText.value,
                callback = object : ZendeskHelper.CreateRequestCallback() {
                    override fun onSuccess() {
                        _isProgressShowing.value = false
                        onSuccess(context)
                    }

                    override fun onError(errorMessage: String?) {
                        _isProgressShowing.value = false
                        onFailure(context, errorMessage)
                    }
                })
        }
    }

    private fun createNewZendeskRequest(
        context: Context,
        description: String,
        callback: ZendeskHelper.CreateRequestCallback
    ) {
        zendeskHelper.createRequest(
            context,
            origin = HelpActivity.Origin.FEEDBACK_FORM,
            selectedSite = null,
            extraTags = listOf("appreview_jetpack", "in_app_feedback"), // matches iOS
            requestDescription = description,
            callback = callback
        )
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
        val message = context.getString(R.string.feedback_form_failure) + "\n$errorMessage"
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

