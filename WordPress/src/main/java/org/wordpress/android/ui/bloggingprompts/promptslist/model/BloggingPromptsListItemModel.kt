package org.wordpress.android.ui.bloggingprompts.promptslist.model

import java.util.Date

data class BloggingPromptsListItemModel(
    val id: Int,
    val text: String,
    val date: Date,
    val isAnswered: Boolean,
    val answersCount: Int,
)
