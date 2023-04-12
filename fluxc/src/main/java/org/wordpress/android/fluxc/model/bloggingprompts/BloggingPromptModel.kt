package org.wordpress.android.fluxc.model.bloggingprompts

import java.util.Date

// TODO #2710: add new response field (at least answered_link) and remove title and content
data class BloggingPromptModel(
    val id: Int,
    val text: String,
    val title: String,
    val content: String,
    val date: Date,
    val isAnswered: Boolean,
    val attribution: String,
    val respondentsCount: Int,
    val respondentsAvatarUrls: List<String>
)
