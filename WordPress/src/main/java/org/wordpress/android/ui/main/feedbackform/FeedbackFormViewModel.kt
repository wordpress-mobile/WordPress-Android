package org.wordpress.android.ui.main.feedbackform

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.support.ZendeskHelper
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.ToastUtilsWrapper
import org.wordpress.android.util.extensions.copyToTempFile
import org.wordpress.android.util.extensions.fileSize
import org.wordpress.android.util.extensions.mimeType
import org.wordpress.android.util.extensions.sizeFmt
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class FeedbackFormViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val zendeskHelper: ZendeskHelper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val toastUtilsWrapper: ToastUtilsWrapper,
    private val feedbackFormUtils: FeedbackFormUtils,
    private val mediaPickerLauncher: MediaPickerLauncher,
) : ScopedViewModel(mainDispatcher) {
    private val _messageText = MutableStateFlow("")
    val messageText = _messageText.asStateFlow()

    private val _isProgressShowing = MutableStateFlow<Boolean?>(null)
    val isProgressShowing = _isProgressShowing.asStateFlow()

    private val _attachments = MutableStateFlow<List<FeedbackFormAttachment>>(emptyList())
    val attachments = _attachments.asStateFlow()

    fun updateMessageText(message: String) {
        if (message != _messageText.value) {
            _messageText.value = message
        }
    }

    fun onSubmitClick(context: Context) {
        if (!NetworkUtils.checkConnection(context)) {
            return
        }

        //  we don't want to prompt the user for their name & email so create an anonymous
        //  identity if it hasn't been previously set
        zendeskHelper.createAnonymousIdentityIfNeeded()

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

    fun onChooseMediaClick(activity: Activity) {
        mediaPickerLauncher.showPhotoPickerForResult(
            activity,
            browserType = MediaBrowserType.FEEDBACK_FORM_SINGLE_MEDIA_PICKER,
            site = selectedSiteRepository.getSelectedSite(),
            localPostId = null
        )
    }

    @Suppress("unused")
    fun addAttachment(uri: Uri, context: Context) {
        val list = _attachments.value
        if (list.size >= MAX_ATTACHMENTS) {
            showToast(R.string.feedback_form_max_attachments_reached)
            return
        }

        val newList = list.toMutableList()
        if (newList.any { it.uri == uri }) {
            showToast(R.string.feedback_form_attachment_already_added)
            return
        }

        val size = uri.fileSize(context)
        if (size > MAX_SINGLE_ATTACHMENT_SIZE) {
            showToast(R.string.feedback_form_attachment_too_large)
            return
        }

        if (totalAttachmentSize() + size > MAX_TOTAL_ATTACHMENT_SIZE) {
            showToast(R.string.feedback_form_total_attachments_too_large)
            return
        }
        val mimeType = uri.mimeType(context)
        val file = uri.copyToTempFile(mimeType, context)
        if (file == null) {
            showToast(R.string.feedback_form_unable_to_create_tempfile)
            return
        }

        if (!feedbackFormUtils.isSupportedAttachmentType(mimeType)) {
            showToast(R.string.feedback_form_unsupported_attachment)
            return
        }

        val attachmentType = when {
            mimeType.startsWith("image") -> FeedbackFormAttachmentType.IMAGE
            mimeType.startsWith("video") -> FeedbackFormAttachmentType.VIDEO
            mimeType.startsWith("audio") -> FeedbackFormAttachmentType.AUDIO
            else -> FeedbackFormAttachmentType.DOCUMENT
        }
        val sizeFmt = uri.sizeFmt(context)
        val counter = newList.filter { it.attachmentType == attachmentType }.size + 1
        val displayName = "${attachmentType}_$counter ($sizeFmt)"

        newList.add(
            FeedbackFormAttachment(
                uri = uri,
                tempFile = file,
                size = size,
                displayName = displayName,
                mimeType = mimeType,
                attachmentType = attachmentType
            )
        )
        _attachments.value = newList.toList()
    }

    fun onRemoveMediaClick(uri: Uri) {
        val list = _attachments.value
        val newList = list.toMutableList()
        if (newList.removeIf { it.uri == uri }) {
            _attachments.value = newList.toList()
        }
    }

    private fun totalAttachmentSize(): Long {
        val list = _attachments.value
        return list.sumOf { it.size }
    }

    private fun showToast(@StringRes msgId: Int) {
        viewModelScope.launch {
            toastUtilsWrapper.showToast(msgId)
        }
    }

    companion object {
        private const val MAX_SINGLE_ATTACHMENT_SIZE = 50000000
        private const val MAX_TOTAL_ATTACHMENT_SIZE = MAX_SINGLE_ATTACHMENT_SIZE * 3
        private const val MAX_ATTACHMENTS = 15
    }
}

