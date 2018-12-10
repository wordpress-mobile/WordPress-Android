package org.wordpress.android.ui.sitecreation

import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.support.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleGeneral
import org.wordpress.android.ui.sitecreation.NewSiteCreationMainVM.NewSiteCreationScreenTitle.ScreenTitleStepCount
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.util.wizard.WizardState
import org.wordpress.android.viewmodel.SingleEventObservable
import javax.inject.Inject

data class SiteCreationState(
    val segmentId: Long? = null,
    val verticalId: String? = null,
    val siteTitle: String? = null,
    val siteTagLine: String? = null
) : WizardState

typealias NavigationTarget = WizardNavigationTarget<SiteCreationStep, SiteCreationState>

class NewSiteCreationMainVM @Inject constructor() : ViewModel() {
    private val wizardManager: WizardManager<SiteCreationStep> = WizardManager(
            // TODO we'll receive this from a server/Firebase config
            listOf(
                    SiteCreationStep.fromString("site_creation_segments"),
                    SiteCreationStep.fromString("site_creation_verticals"),
                    SiteCreationStep.fromString("site_creation_domains")
            )
    )
    private var isStarted = false
    private var siteCreationState = SiteCreationState()

    val navigationTargetObservable: SingleEventObservable<NavigationTarget> = SingleEventObservable(
            Transformations.map(wizardManager.navigatorLiveData) {
                WizardNavigationTarget(it, siteCreationState)
            }
    )

    fun start() {
        if (isStarted) return
        isStarted = true
        wizardManager.showNextStep()
    }

    fun onSegmentSelected(segmentId: Long) {
        siteCreationState = siteCreationState.copy(segmentId = segmentId)
        wizardManager.showNextStep()
    }

    fun onBackPressed() {
        wizardManager.onBackPressed()
    }

    fun onVerticalsScreenFinished(verticalId: String) {
        siteCreationState = siteCreationState.copy(verticalId = verticalId)
        wizardManager.showNextStep()
    }

    fun onSkipClicked() {
        wizardManager.showNextStep()
    }

    fun screenTitleForWizardStep(step: SiteCreationStep): NewSiteCreationScreenTitle {
        val stepPosition = wizardManager.stepPosition(step)
        return if (stepPosition == 1) {
            ScreenTitleGeneral(R.string.new_site_creation_screen_title_general)
        } else {
            ScreenTitleStepCount(
                    R.string.new_site_creation_screen_title_step_count,
                    wizardManager.stepsCount() - 1, // -1 -> first item has general title - Create Site
                    stepPosition - 1 // -1 -> first item has general title - Create Site
            )
        }
    }

    sealed class NewSiteCreationScreenTitle {
        data class ScreenTitleStepCount(@StringRes val resId: Int, val stepsCount: Int, val stepPosition: Int) :
                NewSiteCreationScreenTitle()

        data class ScreenTitleGeneral(@StringRes val resId: Int) :
                NewSiteCreationScreenTitle()
    }
}
