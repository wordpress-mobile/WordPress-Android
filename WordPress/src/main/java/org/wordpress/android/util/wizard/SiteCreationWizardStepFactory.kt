package org.wordpress.android.util.wizard

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import java.util.Arrays
import javax.inject.Inject

/**
 * Marker interface representing a single step/screen in a wizard
 */
interface WizardStep

/**
 * Marker interface representing a state which contains all gathered data from the user input.
 */
interface WizardState

/**
 * Navigation target containing all the data needed for navigating the user to a next screen of the wizard.
 */
class WizardNavigationTarget<S : WizardStep, T : WizardState>(val wizardStepIdentifier: S, val wizardState: T)

class WizardManager<T>(
    private val steps: List<T>
) {
    private val _navigatorLiveData = MutableLiveData<T>()
    val navigatorLiveData: LiveData<T> = _navigatorLiveData
    private var currentStepIndex: Int = -1

    fun showNextStep() {
        if (isIndexValid(++currentStepIndex)) {
            _navigatorLiveData.value = steps[currentStepIndex]
        } else {
            throw IllegalStateException("Invalid index.")
        }
    }

    /**
     * Fragments need to inflate the view in OnCreateView only when getView() != null, otherwise their state
     * gets lost.
     */
    fun onBackPressed() {
        --currentStepIndex
    }

    fun hasNextStep(): Boolean {
        return isIndexValid(currentStepIndex + 1)
    }

    private fun isIndexValid(currentStepIndex: Int): Boolean {
        return currentStepIndex >= 0 && currentStepIndex < steps.size
    }
}

data class SiteCreationState(
    val segmentId: Long? = null,
    val verticalId: Long? = null,
    val siteTitle: String? = null,
    val siteTagline: String? = null
) : WizardState

//*****************************************************************************************************//

class SiteCreationMainVM @Inject constructor() : ViewModel(), SegmentsScreenListener {
    private val wizardManager: WizardManager<SiteCreationStep> = WizardManager(
            // we'll receive this from a server/Firebase config
            Arrays.asList(
                    SiteCreationStep.fromString("site_creation_segments"),
                    SiteCreationStep.fromString("site_creation_verticals")
            )
    )
    private var isStarted = false
    private var siteCreationState = SiteCreationState()

    val navigationTargetObservable: LiveData<WizardNavigationTarget<SiteCreationStep, SiteCreationState>> = Transformations
            .map(wizardManager.navigatorLiveData) { WizardNavigationTarget(it, siteCreationState) }

    fun start() {
        if (isStarted) return
        isStarted = true
        wizardManager.showNextStep()
    }

    fun onBackPressed() {
        wizardManager.onBackPressed()
    }

    override fun onSegmentSelected(segmentId: Long) {
        siteCreationState = siteCreationState.copy(segmentId = segmentId)
        if (wizardManager.hasNextStep()) {
            wizardManager.showNextStep()
        }
    }
}

interface SegmentsScreenListener {
    fun onSegmentSelected(segmentId: Long)
}
