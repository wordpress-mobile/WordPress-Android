package org.wordpress.android.support.he.model

import org.wordpress.android.support.common.model.Conversation
import java.util.Date

data class SupportConversation(
    val id: Long,
    val title: String,
    val description: String,
    val lastMessageSentAt: Date,
    val status: String,
    val messages: List<SupportMessage>
): Conversation {
    override fun getConversationId(): Long = id
}
