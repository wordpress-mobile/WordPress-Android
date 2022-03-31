package org.wordpress.android.ui.bloggingprompts.onboarding

sealed class BloggingPromptsOnboardingAction {
    object OpenEditor : BloggingPromptsOnboardingAction()

    object OpenSitePicker : BloggingPromptsOnboardingAction()

    object OpenRemindersIntro : BloggingPromptsOnboardingAction()
}
