package org.wordpress.android.support.he.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import java.util.Date

@Immutable
data class SupportMessage(
    val id: Long,
    val rawText: String,
    val formattedText: AnnotatedString,
    val createdAt: Date,
    val authorName: String,
    val authorIsUser: Boolean,
    val attachments: List<SupportAttachment>,
)

data class SupportAttachment (
    val id: Long,
    val filename: String,
    val url: String,
    val type: AttachmentType,
)

enum class AttachmentType { Image, Video, Other }
