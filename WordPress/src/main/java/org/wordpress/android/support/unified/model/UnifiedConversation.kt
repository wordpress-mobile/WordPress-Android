package org.wordpress.android.support.unified.model

import org.wordpress.android.support.common.model.Conversation
import java.util.Date

data class UnifiedConversation(
    val id: Long,
    val title: String,
    val description: String,
    val status: String,
    val canAcceptReply: Boolean,
    val createdAt: Date,
    val updatedAt: Date,
    val messages: List<UnifiedMessage>,
) : Conversation {
    override fun getConversationId(): Long = id

    val isBot: Boolean get() = status.equals(STATUS_BOT, ignoreCase = true)

    companion object {
        const val STATUS_BOT = "bot"
    }
}
