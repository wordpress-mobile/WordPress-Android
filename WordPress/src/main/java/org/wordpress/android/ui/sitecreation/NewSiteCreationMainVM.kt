package org.wordpress.android.ui.sitecreation

import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.util.wizard.WizardState
import org.wordpress.android.viewmodel.SingleEventObservable
import java.util.Arrays
import javax.inject.Inject

data class SiteCreationState(
    val segmentId: Long? = null,
    val verticalId: String? = null,
    val siteTitle: String? = null,
    val siteTagLine: String? = null
) : WizardState

typealias NavigationTarget = WizardNavigationTarget<SiteCreationStep, SiteCreationState>

class NewSiteCreationMainVM @Inject constructor() :
        ViewModel() {
    private val wizardManager: WizardManager<SiteCreationStep> = WizardManager(
            // TODO we'll receive this from a server/Firebase config
            Arrays.asList(
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

    fun onVerticalsScreenFinished(verticalId: String?) {
        verticalId?.let {
            siteCreationState = siteCreationState.copy(verticalId = verticalId)
        }
        wizardManager.showNextStep()
    }
}
