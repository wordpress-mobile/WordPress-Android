package org.wordpress.android.support.aibot.model

import java.util.Date

data class BotConversation(
    val id: Long,
    val createdAt: Date,
    val mostRecentMessageDate: Date,
    val lastMessage: String,
    val messages: List<BotMessage>
)
