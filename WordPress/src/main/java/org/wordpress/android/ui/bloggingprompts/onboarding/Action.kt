package org.wordpress.android.ui.bloggingprompts.onboarding

import org.wordpress.android.models.bloggingprompts.BloggingPrompt

sealed class Action {
    data class OpenEditor(val bloggingPrompt: BloggingPrompt) : Action()
}
