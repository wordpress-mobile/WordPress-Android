package org.wordpress.android.ui.bloggingprompts.onboarding

import org.wordpress.android.models.bloggingprompts.BloggingPrompt

sealed class BloggingPromptsOnboardingAction {
    data class OpenEditor(val bloggingPrompt: BloggingPrompt) : BloggingPromptsOnboardingAction()

    object OpenSitePicker : BloggingPromptsOnboardingAction()

    object OpenRemindersIntro : BloggingPromptsOnboardingAction()
}
