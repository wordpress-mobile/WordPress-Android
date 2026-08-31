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
        get() {
            val normalizedContentType = contentType.lowercase()
            return when {
                normalizedContentType.startsWith("image/") -> AttachmentType.Image
                normalizedContentType.startsWith("video/") -> AttachmentType.Video
                normalizedContentType.startsWith("text/html") -> AttachmentType.Link
                else -> AttachmentType.Other
            }
        }
}
