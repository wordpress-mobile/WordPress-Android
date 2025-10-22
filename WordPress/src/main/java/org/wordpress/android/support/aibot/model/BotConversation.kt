package org.wordpress.android.support.aibot.model

import org.wordpress.android.support.common.model.Conversation
import java.util.Date

data class BotConversation(
    val id: Long,
    val createdAt: Date,
    val mostRecentMessageDate: Date,
    val lastMessage: String,
    val messages: List<BotMessage>
): Conversation {
    override fun getConversationId(): Long = id
}
