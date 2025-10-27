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
    val authorIsUser: Boolean
)
