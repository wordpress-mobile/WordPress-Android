package org.wordpress.android.fluxc.model.bloggingprompts

import java.util.Date

data class BloggingPromptModel(
    val id: Int,
    val text: String,
    val title: String,
    val content: String,
    val date: Date,
    val isAnswered: Boolean,
    val attribution: String,
    val respondentsCount: Int,
    val respondentsAvatars: List<String>
)
