package org.wordpress.android.ui.main.feedbackform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.support.ZendeskHelper
import org.wordpress.android.support.ZendeskUploadHelper
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.compose.components.ProgressDialogState
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.ToastUtilsWrapper
import org.wordpress.android.util.extensions.copyToTempFile
import org.wordpress.android.util.extensions.fileSize
import org.wordpress.android.util.extensions.mimeType
import org.wordpress.android.viewmodel.ScopedViewModel
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class FeedbackFormViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val zendeskHelper: ZendeskHelper,
    private val zendeskUploadHelper: ZendeskUploadHelper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val appLogWrapper: AppLogWrapper,
    private val toastUtilsWrapper: ToastUtilsWrapper,
    private val feedbackFormUtils: FeedbackFormUtils,
    private val mediaPickerLauncher: MediaPickerLauncher,
) : ScopedViewModel(mainDispatcher) {
    private val _messageText = MutableStateFlow("")
    val messageText = _messageText.asStateFlow()

    private val _attachments = MutableStateFlow<List<FeedbackFormAttachment>>(emptyList())
    val attachments = _attachments.asStateFlow()

    private val _progressDialogState = MutableStateFlow<ProgressDialogState?>(null)
    val progressDialogState = _progressDialogState.asStateFlow()

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

        // if there are attachments, upload them first to get their tokens, then create the feedback request
        // when they're done uploading
        if (_attachments.value.isNotEmpty()) {
            showProgressDialog(R.string.uploading)
            launch {
                val files = _attachments.value.map { it.tempFile }
                try {
                    val tokens = zendeskUploadHelper.uploadFileAttachments(files)
                    withContext(Dispatchers.Main) {
                        createZendeskFeedbackRequest(
                            context = context,
                            attachmentTokens = tokens
                        )
                    }
                } catch (e: IOException) {
                    hideProgressDialog()
                    onFailure(e.message)
                    return@launch
                }
            }
        } else {
            createZendeskFeedbackRequest(context)
        }
    }

    private fun createZendeskFeedbackRequest(
        context: Context,
        attachmentTokens: List<String> = emptyList()
    ) {
        showProgressDialog(R.string.sending)
        zendeskHelper.createRequest(
            context = context,
            origin = HelpActivity.Origin.FEEDBACK_FORM,
            selectedSite = selectedSiteRepository.getSelectedSite(),
            extraTags = listOf("in_app_feedback"),
            requestDescription = _messageText.value,
            attachmentTokens = attachmentTokens,
            callback = object : ZendeskHelper.CreateRequestCallback() {
                override fun onSuccess() {
                    hideProgressDialog()
                    onSuccess(context)
                }

                override fun onError(errorMessage: String?) {
                    hideProgressDialog()
                    onFailure(errorMessage)
                }
            })
    }

    private fun showProgressDialog(
        @StringRes message: Int
    ) {
        _progressDialogState.value =
            ProgressDialogState(
                message = message,
                showCancel = false,
                dismissible = false
            )
    }

    private fun hideProgressDialog() {
        _progressDialogState.value = null
    }

    fun onCloseClick(context: Context) {
        (context as? Activity)?.let { activity ->
            if (_messageText.value.isEmpty() && _attachments.value.isEmpty()) {
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
        showToast(R.string.feedback_form_success)
        (context as? Activity)?.finish()
    }

    private fun onFailure(errorMessage: String? = null) {
        appLogWrapper.e(T.SUPPORT, "Failed to submit feedback form: $errorMessage")
        showToast(R.string.feedback_form_failure)
    }

    fun onChooseMediaClick(activity: Activity) {
        if (_attachments.value.size >= MAX_ATTACHMENTS) {
            showToast(R.string.feedback_form_max_attachments_reached)
        } else {
            mediaPickerLauncher.showPhotoPickerForResult(
                activity,
                browserType = MediaBrowserType.FEEDBACK_FORM_MEDIA_PICKER,
                site = selectedSiteRepository.getSelectedSite(),
                localPostId = null
            )
        }
    }

    fun onPhotoPickerResult(context: Context, data: Intent) {
        if (data.hasExtra(MediaPickerConstants.EXTRA_MEDIA_URIS)) {
            val stringArray = data.getStringArrayExtra(MediaPickerConstants.EXTRA_MEDIA_URIS)
            stringArray?.forEach { stringUri ->
                // don't add additional attachments if one fails
                if (!addAttachment(context, Uri.parse(stringUri))) {
                    return
                }
            }
        }
    }

    @Suppress("ReturnCount")
    private fun addAttachment(context: Context, uri: Uri): Boolean {
        val list = _attachments.value.toMutableList()
        val fileSize = uri.fileSize(context)
        val mimeType = uri.mimeType(context)

        if (list.size >= MAX_ATTACHMENTS) {
            showToast(R.string.feedback_form_max_attachments_reached)
            return false
        } else if (list.any { it.uri == uri }) {
            showToast(R.string.feedback_form_attachment_already_added)
            return false
        } else if (fileSize > MAX_ATTACHMENT_SIZE) {
            showToast(R.string.feedback_form_attachment_too_large)
            return false
        } else if (!feedbackFormUtils.isSupportedMimeType(mimeType)) {
            showToast(R.string.feedback_form_unsupported_attachment)
            return false
        }

        val file = uri.copyToTempFile(context)
        if (file == null) {
            showToast(R.string.feedback_form_unable_to_create_tempfile)
            return false
        }

        val attachmentType = if (mimeType.startsWith("video")) {
            FeedbackFormAttachmentType.VIDEO
        } else {
            FeedbackFormAttachmentType.IMAGE
        }
        list.add(
            FeedbackFormAttachment(
                uri = uri,
                tempFile = file,
                size = fileSize,
                mimeType = mimeType,
                attachmentType = attachmentType
            )
        )
        _attachments.value = list
        return true
    }

    fun onRemoveMediaClick(uri: Uri) {
        val list = _attachments.value
        val newList = list.toMutableList()
        if (newList.removeIf { it.uri == uri }) {
            _attachments.value = newList.toList()
        }
    }

    private fun showToast(@StringRes msgId: Int) {
        viewModelScope.launch {
            toastUtilsWrapper.showToast(msgId)
        }
    }

    companion object {
        // these match iOS
        private const val MAX_ATTACHMENT_SIZE = 32_000_000
        private const val MAX_ATTACHMENTS = 5
    }
}

