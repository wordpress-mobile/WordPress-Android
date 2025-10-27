package org.wordpress.android.support.aibot.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import java.util.Date

@Immutable
data class BotMessage(
    val id: Long,
    val rawText: String,
    val formattedText: AnnotatedString,
    val date: Date,
    val isWrittenByUser: Boolean
)
