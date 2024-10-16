package org.wordpress.android.fluxc.model.bloggingprompts

import java.util.Date

data class BloggingPromptModel(
    val id: Int,
    val text: String,
    val date: Date,
    val isAnswered: Boolean,
    val attribution: String,
    val respondentsCount: Int,
    val respondentsAvatarUrls: List<String>,
    val answeredLink: String,
    val bloganuaryId: String? = null,
)
