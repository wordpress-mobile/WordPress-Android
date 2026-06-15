package org.wordpress.android.support.unified.model

data class UnifiedAttachment(
    val id: Long,
    val filename: String,
    val contentType: String,
    val url: String,
    val botCitationScore: Float?,
) {
    val isImage: Boolean get() = type == AttachmentType.Image

    val type: AttachmentType
        get() = when {
            contentType.startsWith("image/") -> AttachmentType.Image
            contentType.startsWith("video/") -> AttachmentType.Video
            else -> AttachmentType.Other
        }
}
