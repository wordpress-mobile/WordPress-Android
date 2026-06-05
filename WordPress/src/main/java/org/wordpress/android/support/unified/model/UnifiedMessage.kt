package org.wordpress.android.support.unified.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import java.util.Date

@Immutable
data class UnifiedMessage(
    val id: Long,
    val formattedText: AnnotatedString,
    val authorRole: String,
    val authorName: String,
    val createdAt: Date,
    val attachments: List<UnifiedAttachment>,
) {
    val isUser: Boolean get() = authorRole == AUTHOR_ROLE_USER

    companion object {
        const val AUTHOR_ROLE_USER = "user"
        const val AUTHOR_ROLE_BOT = "bot"
    }
}
