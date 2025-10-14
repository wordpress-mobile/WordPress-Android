package org.wordpress.android.support.feature.aibot.model

import java.util.Date

data class BotMessage(
    val id: Long,
    val text: String,
    val date: Date,
    val isWrittenByUser: Boolean
)
