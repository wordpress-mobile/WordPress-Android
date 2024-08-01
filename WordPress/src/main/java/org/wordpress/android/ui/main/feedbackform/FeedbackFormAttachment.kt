package org.wordpress.android.ui.main.feedbackform

import android.net.Uri
import java.io.File

data class FeedbackFormAttachment(
    val uri: Uri,
    val tempFile: File,
    val displayName: String,
    val mimeType: String,
    val attachmentType: FeedbackFormAttachmentType,
    val size: Long,
)

enum class FeedbackFormAttachmentType {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT
}

/**
 * TODO
 *
fun FeedbackFormAttachment.toZenDeskAttachment(): SupportNetworkService.ZenDeskSupportTicket.Attachment {
    return SupportNetworkService.ZenDeskSupportTicket.Attachment(
        file = this.tempFile,
        type = this.mimeType
    )
}
*/
