package org.wordpress.android.ui.main.feedbackform

import android.net.Uri

data class FeedbackFormAttachment(
    val uri: Uri,
    val mimeType: String,
    val attachmentType: FeedbackFormAttachmentType,
    val size: Long,
)

enum class FeedbackFormAttachmentType {
    IMAGE,
    VIDEO,
}
