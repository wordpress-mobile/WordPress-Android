package org.wordpress.android.ui.bloggingprompts.onboarding

import org.wordpress.android.fluxc.model.SiteModel

sealed class BloggingPromptsOnboardingAction {
    object OpenEditor : BloggingPromptsOnboardingAction()

    data class OpenSitePicker(val selectedSite: SiteModel?) : BloggingPromptsOnboardingAction()

    data class OpenRemindersIntro(
        val selectedSiteLocalId: Int,
        val isFirstTimePublishing: Boolean
    ) : BloggingPromptsOnboardingAction()
}
