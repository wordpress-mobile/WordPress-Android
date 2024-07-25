package org.wordpress.android.ui.main.feedbackform

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.R
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.support.ZendeskHelper
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class FeedbackFormViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val zendeskHelper: ZendeskHelper,
    private val selectedSiteRepository: SelectedSiteRepository,
) : ScopedViewModel(mainDispatcher) {
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

        zendeskHelper.requireIdentity(context, selectedSiteRepository.getSelectedSite()) {
            _isProgressShowing.value = true
            createZendeskFeedbackRequest(
                context = context,
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

    private fun createZendeskFeedbackRequest(
        context: Context,
        callback: ZendeskHelper.CreateRequestCallback
    ) {
        zendeskHelper.createRequest(
            context = context,
            origin = HelpActivity.Origin.FEEDBACK_FORM,
            selectedSite = selectedSiteRepository.getSelectedSite(),
            extraTags = listOf("in_app_feedback"),
            requestDescription = _messageText.value,
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

