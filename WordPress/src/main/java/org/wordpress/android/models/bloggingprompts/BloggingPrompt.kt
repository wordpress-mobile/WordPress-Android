package org.wordpress.android.models.bloggingprompts

data class BloggingPrompt(
    val text: String,
    val content: String,
    val respondents: List<BloggingPromptRespondent>
)
