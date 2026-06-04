package org.wordpress.android.support.unified.model

import org.wordpress.android.support.he.model.AttachmentType

data class UnifiedAttachment(
    val id: Long,
    val filename: String,
    val contentType: String,
    val size: Long,
    val url: String,
    val botCitationScore: Float?,
) {
    val isImage: Boolean get() = contentType.startsWith("image/")

    val type: AttachmentType
        get() = when {
            contentType.startsWith("image/") -> AttachmentType.Image
            contentType.startsWith("video/") -> AttachmentType.Video
            else -> AttachmentType.Other
        }
}
