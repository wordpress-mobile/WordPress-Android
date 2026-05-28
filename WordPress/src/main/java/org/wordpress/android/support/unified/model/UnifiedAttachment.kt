package org.wordpress.android.support.unified.model

data class UnifiedAttachment(
    val id: Long,
    val filename: String,
    val contentType: String,
    val size: Long,
    val url: String,
    val botCitationScore: Float?,
) {
    val isImage: Boolean get() = contentType.startsWith("image/")
}
