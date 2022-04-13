package org.wordpress.android.models.bloggingprompts

data class BloggingPrompt(
    val text: String,
    val template: String,
    val respondents: List<BloggingPromptRespondent>
)
