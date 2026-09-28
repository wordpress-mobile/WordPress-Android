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
    val isUser: Boolean get() = authorRole.equals(AUTHOR_ROLE_USER, ignoreCase = true)

    /**
     * True for the bot's own messages. Once a conversation is escalated to Happiness Engineers, the
     * unified conversations endpoint returns the bot's earlier messages with the raw "bot" author
     * role/name, which the UI replaces with the user-facing assistant name.
     *
     * The author name is matched as well as the role because only the bot carries its role there;
     * a user's name is their login or display name, never "user".
     */
    val isBot: Boolean
        get() = authorRole.equals(AUTHOR_ROLE_BOT, ignoreCase = true) ||
                authorName.equals(AUTHOR_ROLE_BOT, ignoreCase = true)

    /** A Happiness Engineer, i.e. anyone who is neither the user nor the bot. */
    val isSupport: Boolean get() = !isUser && !isBot

    companion object {
        const val AUTHOR_ROLE_USER = "user"
        const val AUTHOR_ROLE_BOT = "bot"
    }
}
