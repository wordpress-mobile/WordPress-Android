package org.wordpress.android.ui.bloggingprompts.onboarding

import org.wordpress.android.fluxc.model.SiteModel

sealed class BloggingPromptsOnboardingAction {
    data class OpenEditor(val promptId: Int) : BloggingPromptsOnboardingAction()

    data class OpenSitePicker(val selectedSite: SiteModel?) : BloggingPromptsOnboardingAction()

    data class OpenRemindersIntro(val selectedSiteLocalId: Int) : BloggingPromptsOnboardingAction()

    object DismissDialog : BloggingPromptsOnboardingAction()

    object DoNothing : BloggingPromptsOnboardingAction()
}
