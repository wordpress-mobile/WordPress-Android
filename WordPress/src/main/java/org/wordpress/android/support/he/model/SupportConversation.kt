package org.wordpress.android.support.he.model

import java.util.Date

data class SupportConversation(
    val id: Long,
    val title: String,
    val description: String,
    val lastMessageSentAt: Date,
    val messages: List<SupportMessage>
)
