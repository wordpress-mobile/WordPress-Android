package org.wordpress.android.models.bloggingprompts

data class BloggingPrompt(
    val id: Int,
    val text: String,
    val content: String,
    val respondents: List<BloggingPromptRespondent>
)
